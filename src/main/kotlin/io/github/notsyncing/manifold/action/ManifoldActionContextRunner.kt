package io.github.notsyncing.manifold.action

import java.util.concurrent.CompletableFuture

class ManifoldActionContextRunner(val sessionIdentifier: String? = null,
                                  val sceneContext: SceneContext? = null) {
    operator fun <R> invoke(a: ManifoldAction<R>, f: (ManifoldAction<R>) -> CompletableFuture<R>): CompletableFuture<R> {
        a.withSession(sessionIdentifier).withContext(sceneContext)

        return f(a)
    }

    operator fun <R> invoke(a: ManifoldAction<R>): CompletableFuture<R> {
        return a.withSession(sessionIdentifier).withContext(sceneContext).execute()
    }
}