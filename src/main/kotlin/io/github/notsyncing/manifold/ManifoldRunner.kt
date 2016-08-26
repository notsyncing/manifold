package io.github.notsyncing.manifold

import java.util.concurrent.CompletableFuture

class ManifoldRunner(val trans: ManifoldTransaction<*>? = null) {
    operator fun <A: ManifoldAction<*, R, F>, R, F> invoke(c: Class<A>): F {
        val f = Manifold.getAction(c).with(trans).core()

        if (f == null) {
            throw RuntimeException("Action $c doesn't specify core!")
        }

        return f
    }

    operator fun <A: ManifoldAction<*, R, F>, R, F> invoke(c: Class<A>, f: (A) -> CompletableFuture<R>): CompletableFuture<R> {
        return Manifold.getAction(c).with(trans).execute(f)
    }
}