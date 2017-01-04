package io.github.notsyncing.manifold.spec.tests.toys.actions

import io.github.notsyncing.manifold.action.ActionMetadata
import io.github.notsyncing.manifold.action.ManifoldAction
import java.util.concurrent.CompletableFuture

@ActionMetadata("B")
class TestActionB : ManifoldAction<String>() {
    override fun action(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("B")
    }
}