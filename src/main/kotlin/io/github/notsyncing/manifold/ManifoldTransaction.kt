package io.github.notsyncing.manifold

import java.util.concurrent.CompletableFuture

abstract class ManifoldTransaction<T>(var body: T) {
    abstract fun begin(withTransaction: Boolean = false): CompletableFuture<Void>
    abstract fun commit(): CompletableFuture<Void>
    abstract fun end(): CompletableFuture<Void>
}