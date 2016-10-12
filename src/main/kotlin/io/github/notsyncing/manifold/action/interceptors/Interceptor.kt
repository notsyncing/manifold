package io.github.notsyncing.manifold.action.interceptors

import java.util.concurrent.CompletableFuture

interface Interceptor<C: InterceptorContext> {
    fun before(context: C): CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)

    fun after(context: C): CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)
}