package io.github.notsyncing.manifold.tests.toys.hooks

import io.github.notsyncing.manifold.hooking.Hook
import java.util.concurrent.CompletableFuture

class TestHook : Hook<Int> {
    override fun execute(inputValue: Int?): CompletableFuture<Int?> {
        if (inputValue == null) {
            return CompletableFuture.completedFuture(1)
        }

        return CompletableFuture.completedFuture(inputValue + 1)
    }
}