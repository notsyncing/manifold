package io.github.notsyncing.manifold.bpmn

interface SuspendableSceneStorageProvider {
    fun offer(state: SuspendableSceneState)
    fun poll(): SuspendableSceneState
    fun getCurrentCount(): Int

    fun reset()
}