package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.di.AutoProvide
import java.util.concurrent.CompletableFuture

class TestAction : ManifoldAction<String, String>(true, true, String::class.java) {
    @AutoProvide
    var testManager: TestManager? = null

    @AutoProvide
    var testManager2: TestManager? = null

    fun hello() = action()

    override fun action(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("Hello")
    }
}