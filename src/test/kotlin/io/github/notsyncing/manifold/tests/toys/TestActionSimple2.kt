package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.tests.toys.interceptor.TestActionInterceptorAnno
import java.util.concurrent.CompletableFuture

@TestActionInterceptorAnno
class TestActionSimple2 : ManifoldAction<String>() {
    override fun action(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("Hello")
    }
}