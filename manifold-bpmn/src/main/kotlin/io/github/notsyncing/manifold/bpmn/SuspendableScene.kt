package io.github.notsyncing.manifold.bpmn

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import java.util.*
import java.util.concurrent.CompletableFuture

abstract class SuspendableScene<R> : ManifoldScene<R>() {
    companion object {
        const val TASK_ID_FIELD = "manifold.scene.suspendable.task.id"
    }

    var suspended = false

    var resumedResults: Map<String, Any?>? = null
    var taskId: String? = null

    protected fun <T: ManifoldAction<*>> awaitFor(waitStrategy: WaitStrategy, vararg actionClasses: Class<T>) {
        SuspendableSceneScheduler.suspend(this, waitStrategy, actionClasses)
        suspended = true
    }

    abstract fun shouldAccept(state: SuspendableSceneState, actionContext: ActionInterceptorContext): Boolean

    abstract fun serialize(): JSONObject
    abstract fun deserialize(o: JSONObject)

    override fun stage(): CompletableFuture<R> {
        if (taskId == null) {
            taskId = UUID.randomUUID().toString()
        }

        return CompletableFuture.completedFuture(null as R)
    }
}