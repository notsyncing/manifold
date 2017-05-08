package io.github.notsyncing.manifold.suspendable.tests.toys

import io.github.notsyncing.manifold.action.ManifoldAction
import java.util.concurrent.CompletableFuture

class TestAction3 : ManifoldAction<Boolean>() {
    companion object {
        var result = false
    }

    override fun action(): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(result)
    }
}