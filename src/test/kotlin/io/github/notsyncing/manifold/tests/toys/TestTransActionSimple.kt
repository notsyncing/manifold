package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldTransactionalAction
import java.util.concurrent.CompletableFuture

class TestTransActionSimple : ManifoldTransactionalAction<String, String>(String::class.java) {
    override fun action(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("Test")
    }
}