package io.github.notsyncing.manifold.suspendable

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.ManifoldScene
import java.util.*
import java.util.concurrent.CompletableFuture

abstract class SuspendableScene<R> : ManifoldScene<R>() {
    companion object {
        const val TASK_ID_FIELD = "manifold.scene.suspendable.task.id"

        val SUSPENDED = Any()
    }

    var suspendAdditionalData: Map<String, Any?>? = null

    var resumedState: SuspendableSceneState? = null
    var taskId: String? = null

    protected open fun <T: ManifoldAction<*>> awaitFor(waitStrategy: WaitStrategy, vararg actionClasses: Class<T>): Any {
        SuspendableSceneScheduler.suspend(this, waitStrategy, actionClasses, suspendAdditionalData)
        suspendAdditionalData = null
        return SUSPENDED
    }

    abstract fun serialize(): JSONObject
    abstract fun deserialize(o: JSONObject)

    override fun stage(): CompletableFuture<R> {
        if (taskId == null) {
            taskId = UUID.randomUUID().toString()
        }

        return CompletableFuture.completedFuture(null as R)
    }
}