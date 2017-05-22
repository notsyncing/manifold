package io.github.notsyncing.manifold.suspendable

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

object SuspendableSceneScheduler {
    private val inMemoryStorageProvider = InMemorySuspendableSceneStorage()

    var storageProvider: SuspendableSceneStorageProvider = InMemorySuspendableSceneStorage()
        set(p) {
            field = p
            p.onTimeout(this::stateTimeoutHandler)
        }

    private val taskCallbacks = ConcurrentHashMap<String, (Any?, Throwable?) -> Unit>()
    private val taskLatestResults = ConcurrentHashMap<String, Pair<Any?, Throwable?>>()
    private val taskCallbackLocks = ConcurrentHashMap<String, Semaphore>()
    private val taskInProgress = ConcurrentHashMap<String, Boolean>()

    init {
        Manifold.addOnResetListener {
            reset()
        }

        storageProvider.onTimeout(this::stateTimeoutHandler)
    }

    private fun stateTimeoutHandler(state: SuspendableSceneState) {
        removeTask(state.sceneTaskId ?: "")
    }

    private fun getStorageProvider(scene: SuspendableScene<*>): SuspendableSceneStorageProvider {
        if (scene.noPersist) {
            if (storageProvider is InMemorySuspendableSceneStorage) {
                return storageProvider
            } else {
                return inMemoryStorageProvider
            }
        } else {
            return storageProvider
        }
    }

    fun <T: ManifoldAction<*>> suspend(scene: SuspendableScene<*>, waitStrategy: WaitStrategy,
                                       awaitActionClasses: Array<out Class<T>>,
                                       timeout: Long, timeoutUnit: TimeUnit,
                                       additionalData: Map<String, Any?>? = null) {
        val aw = SuspendableSceneState.AwaitingAction(waitStrategy, false,
                awaitActionClasses.associateBy({ it.name }, { WaitActionResult() }).toMutableMap())

        if (additionalData != null) {
            aw.additionalData.putAll(additionalData)
        }

        val storage = getStorageProvider(scene)
        var state: SuspendableSceneState? = storage.getByTaskId(scene.taskId!!)

        if (state == null) {
            state = SuspendableSceneState()
            state.sceneClassFullName = scene.javaClass.name
            state.sceneState = scene.serialize()
            state.awaitingActions.add(aw)
            state.sceneSessionId = scene.sessionIdentifier
            state.sceneTaskId = scene.taskId
            state.timeout = timeout
            state.timeoutUnit = timeoutUnit

            storage.offer(state)
        } else {
            state.sceneState = scene.serialize()
            state.awaitingActions.add(aw)
        }
    }

    fun resume(state: SuspendableSceneState): CompletableFuture<Any?> {
        val sceneClass = Class.forName(state.sceneClassFullName)
        val scene = sceneClass.newInstance() as SuspendableScene<*>
        scene.resumedState = state
        scene.taskId = state.sceneTaskId

        if ((state.awaitingActions.size == 1) && (state.awaitingActions[0].results.size == 1)) {
            val r = state.awaitingActions[0].results.values.firstOrNull()
            scene.lastResult = r?.result
            scene.lastException = r?.exception
        } else {
            scene.lastResult = null
            scene.lastException = null
        }

        scene.deserialize(state.sceneState)

        return Manifold.run(scene, state.sceneSessionId) as CompletableFuture<Any?>
    }

    private fun executeTaskCallback(taskId: String, r: Any?, ex: Throwable?) {
        val lock = taskCallbackLocks[taskId]!!
        lock.acquire()

        try {
            val callback = taskCallbacks.remove(taskId)

            if (callback != null) {
                CompletableFuture.runAsync { callback(r, ex) }
            } else {
                taskLatestResults[taskId] = Pair(r, ex)
            }
        } finally {
            lock.release()
        }
    }

    fun onActionExecuted(actionContext: ActionInterceptorContext) {
        CompletableFuture.runAsync {
            try {
                if ((storageProvider.getCurrentCount() <= 0) && (inMemoryStorageProvider.getCurrentCount() <= 0)) {
                    return@runAsync
                }

                val actionClassName = actionContext.action.javaClass.name
                val forTaskId = actionContext.action.context.additionalData[SuspendableScene.TASK_ID_FIELD] as String?

                if (forTaskId == null) {
                    return@runAsync
                }

                taskInProgress[forTaskId] = true

                var state = inMemoryStorageProvider.pollByTaskId(forTaskId)
                var stateInMemory = true

                if (state == null) {
                    state = storageProvider.pollByTaskId(forTaskId)
                    stateInMemory = false
                }

                if (state == null) {
                    return@runAsync
                }

                f@ for (aw in state.awaitingActions) {
                    val r = aw.results[actionClassName]

                    if ((r == null) || (r.executed)) {
                        continue
                    }

                    r.executed = true
                    r.result = actionContext.result
                    r.exception = actionContext.exception

                    when (aw.waitStrategy) {
                        WaitStrategy.And -> {
                            if (aw.results.all { (_, result) -> result.executed }) {
                                aw.passed = true
                                continue@f
                            }
                        }

                        WaitStrategy.Or -> {
                            aw.passed = true
                            continue@f
                        }
                    }
                }

                if (state.awaitingActions.all { it.passed }) {
                    resume(state)
                            .thenAccept {
                                executeTaskCallback(forTaskId, it, null)
                            }
                            .exceptionally {
                                executeTaskCallback(forTaskId, null, it)
                                null
                            }
                            .thenAccept {
                                if (!(taskInProgress[forTaskId] ?: false)) {
                                    removeTask(forTaskId)
                                }
                            }
                } else {
                    if (stateInMemory) {
                        inMemoryStorageProvider.offer(state)
                    } else {
                        storageProvider.offer(state)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reset() {
        storageProvider.reset()
        inMemoryStorageProvider.reset()
        taskCallbacks.clear()
        taskLatestResults.clear()
        taskCallbackLocks.clear()
        taskInProgress.clear()
    }

    fun hasAnyTaskWaitingForAction(taskId: String, actionClassName: String): Boolean {
        val state = inMemoryStorageProvider.getByTaskId(taskId) ?: storageProvider.getByTaskId(taskId)

        if (state == null) {
            return false
        }

        return state.awaitingActions.any { it.results.containsKey(actionClassName) }
    }

    private fun registerTaskStepCallback(taskId: String, callback: (Any?, Throwable?) -> Unit) {
        val lock = taskCallbackLocks[taskId]!!
        lock.acquire()

        try {
            val currResult = taskLatestResults.remove(taskId)

            if (currResult != null) {
                val (r, ex) = currResult
                CompletableFuture.runAsync { callback(r, ex) }
            } else {
                taskCallbacks[taskId] = callback
            }
        } finally {
            lock.release()
        }
    }

    fun taskStep(taskId: String): CompletableFuture<Pair<Boolean, Any?>> {
        val f = CompletableFuture<Pair<Boolean, Any?>>()

        registerTaskStepCallback(taskId) { r, ex ->
            if (ex != null) {
                f.completeExceptionally(ex)
            } else {
                f.complete(Pair(!(taskInProgress[taskId] ?: false), r))
            }
        }

        return f
    }

    fun onTaskCreated(taskId: String) {
        taskCallbackLocks[taskId] = Semaphore(1)
        taskInProgress[taskId] = true
    }

    fun taskDone(taskId: String) {
        taskInProgress.remove(taskId)
    }

    fun removeTask(taskId: String) {
        taskCallbacks.remove(taskId)
        taskLatestResults.remove(taskId)
        taskCallbackLocks.remove(taskId)
    }

    fun getTaskSceneName(taskId: String): String? {
        val state = inMemoryStorageProvider.getByTaskId(taskId) ?: storageProvider.getByTaskId(taskId)

        if (state == null) {
            return null
        }

        val sceneClass = Class.forName(state.sceneClassFullName)

        return sceneClass.getAnnotation(SceneMetadata::class.java)?.value
    }

    fun isTaskDone(taskId: String): Boolean {
        return !taskInProgress.containsKey(taskId)
    }
}