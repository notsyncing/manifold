package io.github.notsyncing.manifold.suspendable

import io.github.notsyncing.manifold.action.interceptors.ActionInterceptor
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import io.github.notsyncing.manifold.action.interceptors.ForEveryAction
import io.github.notsyncing.manifold.action.interceptors.InterceptorException
import java.util.concurrent.CompletableFuture

@ForEveryAction
class ActionExecutionTrigger : ActionInterceptor() {
    override fun before(context: ActionInterceptorContext): CompletableFuture<Unit> {
        val taskId = context.action.context.additionalData[SuspendableScene.TASK_ID_FIELD] as String?

        if (taskId == null) {
            return super.before(context)
        }

        if (!SuspendableSceneScheduler.hasAnyTaskWaitingForAction(taskId, context.action.javaClass.name)) {
            context.stop(InterceptorException("Action ${context.action} contains a task id $taskId, but no suspendable scene is waiting for it!"))
        }

        return super.before(context)
    }

    override fun after(context: ActionInterceptorContext): CompletableFuture<Unit> {
        SuspendableSceneScheduler.onActionExecuted(context)
        return super.after(context)
    }
}