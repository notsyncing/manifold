package io.github.notsyncing.manifold.bpmn.tests.toys

import io.github.notsyncing.manifold.action.ManifoldAction
import java.util.concurrent.CompletableFuture

class TestAction2 : ManifoldAction<String>() {
    override fun action(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("TestAction2")
    }
}