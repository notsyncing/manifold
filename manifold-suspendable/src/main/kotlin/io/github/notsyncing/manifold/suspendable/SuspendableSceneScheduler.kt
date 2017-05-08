package io.github.notsyncing.manifold.suspendable

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import java.util.concurrent.CompletableFuture

object SuspendableSceneScheduler {
    var storageProvider: SuspendableSceneStorageProvider = InMemorySuspendableSceneStorage()

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
        CompletableFuture.runAsync {
            try {
                if (storageProvider.getCurrentCount() <= 0) {
                    return@runAsync
                }

                val actionClassName = actionContext.action.javaClass.name
                val forTaskId = actionContext.action.context.additionalData[SuspendableScene.TASK_ID_FIELD] as String?

                if (forTaskId == null) {
                    return@runAsync
                }

                val state = storageProvider.pollByTaskId(forTaskId)

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
                } else {
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