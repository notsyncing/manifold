package io.github.notsyncing.manifold.action

import java.util.concurrent.CompletableFuture

class ManifoldRunner(val trans: ManifoldTransaction<*>? = null) {
    operator fun <A: ManifoldAction<*, R>, R> invoke(a: A, f: (A) -> CompletableFuture<R>): CompletableFuture<R> {
        a.withTransaction(trans)

        return f(a)
    }

    operator fun <A: ManifoldAction<*, R>, R> invoke(a: A): CompletableFuture<R> {
        return a.withTransaction(trans).execute()
    }
}