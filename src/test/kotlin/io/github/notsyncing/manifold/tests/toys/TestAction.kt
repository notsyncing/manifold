package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.ManifoldAction
import java.util.concurrent.CompletableFuture

class TestAction(var testManager: TestManager?,
                 var testManager2: TestManager?) : ManifoldAction<String, String>() {
    fun hello(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("Hello")
    }
}