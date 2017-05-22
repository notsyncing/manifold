package io.github.notsyncing.manifold.suspendable

interface SuspendableSceneStorageProvider {
    fun offer(state: SuspendableSceneState)
    fun getCurrentCount(): Int
    fun getByTaskId(taskId: String): SuspendableSceneState?
    fun pollByTaskId(taskId: String): SuspendableSceneState?
    fun onTimeout(handler: (SuspendableSceneState) -> Unit)

    fun reset()
}