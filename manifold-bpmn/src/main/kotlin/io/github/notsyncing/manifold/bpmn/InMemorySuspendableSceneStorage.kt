package io.github.notsyncing.manifold.bpmn

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class InMemorySuspendableSceneStorage : SuspendableSceneStorageProvider {
    private val sceneQueue = LinkedBlockingQueue<SuspendableSceneState>()
    private val sceneAccessor = ConcurrentHashMap<String, SuspendableSceneState>()

    override fun offer(state: SuspendableSceneState) {
        sceneQueue.offer(state)
        sceneAccessor[state.sceneTaskId!!] = state
    }

    override fun poll(): SuspendableSceneState? {
        val r = sceneQueue.poll()

        if (r != null) {
            sceneAccessor.remove(r.sceneTaskId!!)
        }

        return r
    }

    override fun getCurrentCount(): Int {
        return sceneQueue.size
    }

    fun getAsList(): List<SuspendableSceneState> {
        return sceneQueue.toList()
    }

    override fun getByTaskId(taskId: String): SuspendableSceneState? {
        return sceneAccessor[taskId]
    }

    override fun reset() {
        sceneQueue.clear()
        sceneAccessor.clear()
    }
}