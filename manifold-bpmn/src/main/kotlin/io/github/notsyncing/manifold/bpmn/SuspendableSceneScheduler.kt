package io.github.notsyncing.manifold.bpmn

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

    fun <T: ManifoldAction<*>> suspend(scene: SuspendableScene<*>, waitStrategy: WaitStrategy, awaitActionClasses: Array<out Class<T>>) {
        val state = SuspendableSceneState()
        state.sceneClassFullName = scene.javaClass.name
        state.awaitStrategy = waitStrategy
        state.sceneState = scene.serialize()
        state.awaitingActions = awaitActionClasses.associateBy({ it.name }, { ActionResult() })
        state.sceneSessionId = scene.sessionIdentifier
        state.sceneTaskId = scene.taskId

        storageProvider.offer(state)
    }

    fun resume(state: SuspendableSceneState) {
        val sceneClass = Class.forName(state.sceneClassFullName)
        val scene = sceneClass.newInstance() as SuspendableScene<*>
        scene.resumedResults = state.awaitingActions.mapValues { it.value.result }
        scene.taskId = state.sceneTaskId
        scene.deserialize(state.sceneState)

        Manifold.run(scene, state.sceneSessionId)
    }

    fun onActionExecuted(actionContext: ActionInterceptorContext) {
        worker.submit {
            try {
                if (storageProvider.getCurrentCount() <= 0) {
                    return@submit
                }

                val actionClassName = actionContext.action.javaClass.name
                val forTaskId = actionContext.action.context.additionalData[SuspendableScene.TASK_ID_FIELD]

                if (forTaskId == null) {
                    return@submit
                }

                f@ for (i in 1..storageProvider.getCurrentCount()) {
                    val state = storageProvider.poll()

                    if (forTaskId != state.sceneTaskId) {
                        storageProvider.offer(state)
                        continue
                    }

                    val r = state.awaitingActions[actionClassName]

                    if ((r != null) && (!r.executed)) {
                        val sceneClass = Class.forName(state.sceneClassFullName)
                        val scene = sceneClass.newInstance() as SuspendableScene<*>

                        if (scene.shouldAccept(state, actionContext)) {
                            r.executed = true
                            r.result = actionContext.result

                            when (state.awaitStrategy) {
                                WaitStrategy.And -> {
                                    if (state.awaitingActions.all { (_, result) -> result.executed }) {
                                        CompletableFuture.runAsync { resume(state) }
                                        continue@f
                                    }
                                }

                                WaitStrategy.Or -> {
                                    CompletableFuture.runAsync { resume(state) }
                                    continue@f
                                }
                            }
                        }
                    }

                    storageProvider.offer(state)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reset() {
        storageProvider.reset()
    }
}