package io.github.notsyncing.manifold.bpmn

import java.util.concurrent.LinkedBlockingQueue

class InMemorySuspendableSceneStorage : SuspendableSceneStorageProvider {
    private val sceneQueue = LinkedBlockingQueue<SuspendableSceneState>()

    override fun offer(state: SuspendableSceneState) {
        sceneQueue.offer(state)
    }

    override fun poll(): SuspendableSceneState {
        return sceneQueue.poll()
    }

    override fun getCurrentCount(): Int {
        return sceneQueue.size
    }

    fun getAsList(): List<SuspendableSceneState> {
        return sceneQueue.toList()
    }

    override fun reset() {
        sceneQueue.clear()
    }
}