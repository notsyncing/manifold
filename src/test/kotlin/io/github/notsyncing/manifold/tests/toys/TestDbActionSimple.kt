package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldDatabaseAction
import java.util.concurrent.CompletableFuture

class TestDbActionSimple : ManifoldDatabaseAction<String, String>(String::class.java) {
    override fun action(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("Test")
    }
}