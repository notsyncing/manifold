package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.ManifoldRunnerContext
import java.util.concurrent.CompletableFuture

class TestAction(var testManager: TestManager?,
                 var testManager2: TestManager?) : ManifoldAction<String, String, () -> CompletableFuture<String>, ManifoldRunnerContext>(true, true, String::class.java) {
    override fun core() = this::hello

    fun hello(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("Hello")
    }
}