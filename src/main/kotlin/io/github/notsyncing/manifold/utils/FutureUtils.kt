package io.github.notsyncing.manifold.utils

import java.util.concurrent.CompletableFuture

object FutureUtils {
    fun <T> failed(ex: Throwable): CompletableFuture<T> {
        val c = CompletableFuture<T>()
        c.completeExceptionally(ex)
        return c
    }
}