package io.github.notsyncing.manifold.action.interceptors

import java.util.concurrent.CompletableFuture

abstract class Interceptor<C: InterceptorContext> {
    open fun before(context: C): CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)

    open fun after(context: C): CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)
}