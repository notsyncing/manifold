package io.github.notsyncing.manifold.suspendable

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class InMemorySuspendableSceneStorage : SuspendableSceneStorageProvider {
    private val sceneAccessor = ConcurrentHashMap<String, SuspendableSceneState>()
    private var timeoutHandler: ((SuspendableSceneState) -> Unit)? = null

    private val scheduler = Executors.newSingleThreadScheduledExecutor {
        val t = Thread()
        t.isDaemon = true
        t
    }

    override fun offer(state: SuspendableSceneState) {
        sceneAccessor[state.sceneTaskId!!] = state

        if (state.timeout > 0) {
            scheduler.schedule({
                val s = sceneAccessor.remove(state.sceneTaskId!!)

                if (s != null) {
                    timeoutHandler?.invoke(s)
                }
            }, state.timeout, state.timeoutUnit)
        }
    }

    override fun getCurrentCount(): Int {
        return sceneAccessor.size
    }

    fun getAsList(): List<SuspendableSceneState> {
        return sceneAccessor.values.toList()
    }

    override fun getByTaskId(taskId: String): SuspendableSceneState? {
        return sceneAccessor[taskId]
    }

    override fun pollByTaskId(taskId: String): SuspendableSceneState? {
        return sceneAccessor.remove(taskId)
    }

    override fun reset() {
        sceneAccessor.clear()
    }

    override fun onTimeout(handler: (SuspendableSceneState) -> Unit) {
        timeoutHandler = handler
    }
}