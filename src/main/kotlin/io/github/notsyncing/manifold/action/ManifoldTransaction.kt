package io.github.notsyncing.manifold.action

import java.util.concurrent.CompletableFuture

abstract class ManifoldTransaction<T>(var body: T) {
    abstract fun begin(withTransaction: Boolean = false): CompletableFuture<Void>
    abstract fun commit(endTransaction: Boolean = true): CompletableFuture<Void>
    abstract fun end(): CompletableFuture<Void>
}