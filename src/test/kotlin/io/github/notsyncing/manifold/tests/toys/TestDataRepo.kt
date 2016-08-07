package io.github.notsyncing.manifold.tests.toys

import java.util.concurrent.CompletableFuture

class TestDataRepo {
    fun beginTransaction(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    fun commit(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    fun rollback(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    fun end(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }
}