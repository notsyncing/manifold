package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import java.util.concurrent.CompletableFuture

class ManifoldRunner(val trans: ManifoldTransaction<*>? = null, var context: ManifoldRunnerContext? = null) {
    operator fun <A: ManifoldAction<*, *, F, C>, F, C: ManifoldRunnerContext> invoke(c: Class<A>): F {
        val f = Manifold.getAction(c).with(context as C?).with(trans).core()

        if (f == null) {
            throw RuntimeException("Action $c doesn't specify core!")
        }

        return f
    }

    operator fun <A: ManifoldAction<*, R, *, C>, R, C: ManifoldRunnerContext> invoke(c: Class<A>, f: (A) -> CompletableFuture<R>): CompletableFuture<R> {
        return Manifold.getAction(c).with(context as C?).with(trans).execute(f)
    }
}