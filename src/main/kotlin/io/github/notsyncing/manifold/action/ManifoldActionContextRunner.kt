package io.github.notsyncing.manifold.action

import java.util.concurrent.CompletableFuture

class ManifoldActionContextRunner(val sessionIdentifier: String? = null, val trans: ManifoldTransaction<*>? = null) {
    operator fun <R> invoke(a: ManifoldAction<R>, f: (ManifoldAction<R>) -> CompletableFuture<R>): CompletableFuture<R> {
        a.withTransaction(trans).withSession(sessionIdentifier)

        return f(a)
    }

    operator fun <R> invoke(a: ManifoldAction<R>): CompletableFuture<R> {
        return a.withTransaction(trans).withSession(sessionIdentifier).execute()
    }
}