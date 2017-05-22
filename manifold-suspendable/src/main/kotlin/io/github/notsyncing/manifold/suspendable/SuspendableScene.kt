package io.github.notsyncing.manifold.suspendable

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.ManifoldScene
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

abstract class SuspendableScene<R>(var noPersist: Boolean = false) : ManifoldScene<R>() {
    companion object {
        const val TASK_ID_FIELD = "manifold.scene.suspendable.task.id"

        val SUSPENDED = Any()
    }

    var suspendAdditionalData: Map<String, Any?>? = null

    var resumedState: SuspendableSceneState? = null
    var lastResult: Any? = null
    var lastException: Throwable? = null
    var taskId: String? = null

    open fun <T: ManifoldAction<*>> awaitFor(waitStrategy: WaitStrategy, timeout: Long, timeoutUnit: TimeUnit, vararg actionClasses: Class<T>): Any {
        SuspendableSceneScheduler.suspend(this, waitStrategy, actionClasses, timeout, timeoutUnit, suspendAdditionalData)
        suspendAdditionalData = null
        return SUSPENDED
    }

    open fun <T: ManifoldAction<*>> awaitFor(waitStrategy: WaitStrategy, vararg actionClasses: Class<T>): Any {
        return awaitFor(waitStrategy, 10, TimeUnit.MINUTES, *actionClasses)
    }

    open fun <T: ManifoldAction<*>> awaitForever(waitStrategy: WaitStrategy, vararg actionClasses: Class<T>): Any {
        return awaitFor(waitStrategy, 0, TimeUnit.MILLISECONDS, *actionClasses)
    }

    open fun <T: ManifoldAction<*>> awaitFor(actionClasses: Class<T>): Any {
        return awaitFor(WaitStrategy.And, actionClasses)
    }

    abstract fun serialize(): JSONObject
    abstract fun deserialize(o: JSONObject)

    override fun stage(): CompletableFuture<R> {
        if (taskId == null) {
            taskId = UUID.randomUUID().toString()
        }

        SuspendableSceneScheduler.onTaskCreated(taskId!!)

        return CompletableFuture.completedFuture(null as R)
    }

    fun done() {
        SuspendableSceneScheduler.taskDone(taskId!!)
    }
}