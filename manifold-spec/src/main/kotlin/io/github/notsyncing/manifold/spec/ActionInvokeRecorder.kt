package io.github.notsyncing.manifold.spec

import io.github.notsyncing.manifold.action.interceptors.ActionInterceptor
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import io.github.notsyncing.manifold.action.interceptors.ForEveryAction
import io.github.notsyncing.manifold.spec.annotations.ActionMetadata
import java.util.concurrent.CompletableFuture

@ForEveryAction
class ActionInvokeRecorder : ActionInterceptor() {
    companion object {
        val recorded = mutableListOf<Pair<String, Any?>>()

        fun reset() {
            recorded.clear()
        }
    }

    private var metadata: ActionMetadata? = null

    override fun before(context: ActionInterceptorContext): CompletableFuture<Unit> {
        metadata = context.action.javaClass.getAnnotation(ActionMetadata::class.java)

        if (metadata == null) {
            println("Action ${context.action.javaClass} has no @${ActionMetadata::class.simpleName} annotation present!")
        }

        return super.before(context)
    }

    override fun after(context: ActionInterceptorContext): CompletableFuture<Unit> {
        recorded.add(Pair(metadata?.value ?: context.action.javaClass.simpleName, context.result))

        return super.after(context)
    }
}