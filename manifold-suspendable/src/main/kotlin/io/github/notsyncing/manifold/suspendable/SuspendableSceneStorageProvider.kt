package io.github.notsyncing.manifold.suspendable

interface SuspendableSceneStorageProvider {
    fun offer(state: SuspendableSceneState)
    fun poll(): SuspendableSceneState?
    fun getCurrentCount(): Int
    fun getByTaskId(taskId: String): SuspendableSceneState?

    fun reset()
}