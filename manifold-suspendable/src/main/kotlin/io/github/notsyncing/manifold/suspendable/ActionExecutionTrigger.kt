package io.github.notsyncing.manifold.suspendable

import io.github.notsyncing.manifold.action.interceptors.ActionInterceptor
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import io.github.notsyncing.manifold.action.interceptors.ForEveryAction
import java.util.concurrent.CompletableFuture

@ForEveryAction
class ActionExecutionTrigger : ActionInterceptor() {
    override fun after(context: ActionInterceptorContext): CompletableFuture<Unit> {
        SuspendableSceneScheduler.onActionExecuted(context)
        return super.after(context)
    }
}