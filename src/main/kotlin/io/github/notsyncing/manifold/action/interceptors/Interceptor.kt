package io.github.notsyncing.manifold.action.interceptors

import java.util.concurrent.CompletableFuture

abstract class Interceptor<C: InterceptorContext> {
    open fun before(context: C): CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)

    open fun after(context: C): CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)

    open fun destroy(context: C): CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)

    protected fun C.stop(e: Exception? = null) {
        this.interceptorResult = InterceptorResult.Stop
        this.exception = e

        return
    }
}