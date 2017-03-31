package io.github.notsyncing.manifold.bpmn.tests.toys

import io.github.notsyncing.manifold.action.ManifoldAction
import java.util.concurrent.CompletableFuture

class TestAction1 : ManifoldAction<String>() {
    override fun action(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("TestAction1")
    }
}