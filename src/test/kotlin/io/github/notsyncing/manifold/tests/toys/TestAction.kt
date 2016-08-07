package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.ManifoldAction
import io.github.notsyncing.manifold.annotations.AutoProvide
import java.util.concurrent.CompletableFuture

class TestAction() : ManifoldAction<String>() {
    companion object {
        @AutoProvide
        var testManager: TestManager? = null

        var testManager2: TestManager? = null
    }

    fun hello(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("Hello")
    }
}