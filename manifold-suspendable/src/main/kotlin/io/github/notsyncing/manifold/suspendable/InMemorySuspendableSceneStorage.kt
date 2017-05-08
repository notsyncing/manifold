package io.github.notsyncing.manifold.suspendable

import java.util.concurrent.ConcurrentHashMap

class InMemorySuspendableSceneStorage : SuspendableSceneStorageProvider {
    private val sceneAccessor = ConcurrentHashMap<String, SuspendableSceneState>()

    override fun offer(state: SuspendableSceneState) {
        sceneAccessor[state.sceneTaskId!!] = state
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
}