package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldDatabaseAction
import io.github.notsyncing.manifold.action.session.SessionVariable
import io.github.notsyncing.manifold.di.AutoProvide
import java.util.concurrent.CompletableFuture

class TestAction : ManifoldDatabaseAction<String, String>(String::class.java) {
    @AutoProvide
    var testManager: TestManager? = null

    @AutoProvide
    var testManager2: TestManager? = null

    var testVar: String? by SessionVariable<String?>()

    override fun action(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("Hello")
    }
}