package io.github.notsyncing.manifold.spec

import io.github.notsyncing.manifold.action.interceptors.ActionInterceptor
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import io.github.notsyncing.manifold.action.interceptors.ForEveryAction
import io.github.notsyncing.manifold.spec.annotations.ActionMetadata
import java.util.concurrent.CompletableFuture

@ForEveryAction
class ActionInvokeListener : ActionInterceptor() {
    companion object {
        val recorded = mutableListOf<Pair<String, Any?>>()

        fun reset() {
            recorded.clear()
        }
    }

    private lateinit var metadata: ActionMetadata

    override fun before(context: ActionInterceptorContext): CompletableFuture<Unit> {
        metadata = context.action.javaClass.getAnnotation(ActionMetadata::class.java)

        return super.before(context)
    }

    override fun after(context: ActionInterceptorContext): CompletableFuture<Unit> {
        recorded.add(Pair(metadata.value, context.result))

        return super.after(context)
    }
}