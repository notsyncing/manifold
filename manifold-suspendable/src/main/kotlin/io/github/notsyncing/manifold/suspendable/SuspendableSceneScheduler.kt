package io.github.notsyncing.manifold.suspendable

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

object SuspendableSceneScheduler {
    var storageProvider: SuspendableSceneStorageProvider = InMemorySuspendableSceneStorage()

    private val worker = Executors.newSingleThreadExecutor {
        Thread(it).apply { this.isDaemon = true }
    }

    init {
        Manifold.addOnResetListener {
            reset()
        }
    }

    fun <T: ManifoldAction<*>> suspend(scene: SuspendableScene<*>, waitStrategy: WaitStrategy,
                                       awaitActionClasses: Array<out Class<T>>,
                                       additionalData: Map<String, Any?>? = null) {
        val aw = SuspendableSceneState.AwaitingAction(waitStrategy, false,
                awaitActionClasses.associateBy({ it.name }, { ActionResult() }).toMutableMap())

        if (additionalData != null) {
            aw.additionalData.putAll(additionalData)
        }

        var state: SuspendableSceneState? = SuspendableSceneScheduler.storageProvider.getByTaskId(scene.taskId!!)

        if (state == null) {
            state = SuspendableSceneState()
            state.sceneClassFullName = scene.javaClass.name
            state.sceneState = scene.serialize()
            state.awaitingActions.add(aw)
            state.sceneSessionId = scene.sessionIdentifier
            state.sceneTaskId = scene.taskId

            SuspendableSceneScheduler.storageProvider.offer(state)
        } else {
            state.sceneState = scene.serialize()
            state.awaitingActions.add(aw)
        }
    }

    fun resume(state: SuspendableSceneState) {
        val sceneClass = Class.forName(state.sceneClassFullName)
        val scene = sceneClass.newInstance() as SuspendableScene<*>
        scene.resumedState = state
        scene.taskId = state.sceneTaskId
        scene.deserialize(state.sceneState)

        Manifold.run(scene, state.sceneSessionId)
    }

    fun onActionExecuted(actionContext: ActionInterceptorContext) {
        SuspendableSceneScheduler.worker.submit {
            try {
                if (SuspendableSceneScheduler.storageProvider.getCurrentCount() <= 0) {
                    return@submit
                }

                val actionClassName = actionContext.action.javaClass.name
                val forTaskId = actionContext.action.context.additionalData[SuspendableScene.TASK_ID_FIELD]

                if (forTaskId == null) {
                    return@submit
                }

                for (i in 1..SuspendableSceneScheduler.storageProvider.getCurrentCount()) {
                    val state = SuspendableSceneScheduler.storageProvider.poll()!!

                    if (forTaskId != state.sceneTaskId) {
                        SuspendableSceneScheduler.storageProvider.offer(state)
                        continue
                    }

                    f@ for (aw in state.awaitingActions) {
                        val r = aw.results[actionClassName]

                        if ((r != null) && (!r.executed)) {
                            val sceneClass = Class.forName(state.sceneClassFullName)
                            val scene = sceneClass.newInstance() as SuspendableScene<*>

                            if (scene.shouldAccept(state, actionContext)) {
                                r.executed = true
                                r.result = actionContext.result

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
                        }
                    }

                    if (state.awaitingActions.all { it.passed }) {
                        CompletableFuture.runAsync { SuspendableSceneScheduler.resume(state) }
                    } else {
                        SuspendableSceneScheduler.storageProvider.offer(state)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reset() {
        SuspendableSceneScheduler.storageProvider.reset()
    }
}