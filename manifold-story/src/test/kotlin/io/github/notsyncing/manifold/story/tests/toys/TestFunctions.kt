package io.github.notsyncing.manifold.story.tests.toys

import io.github.notsyncing.manifold.utils.FutureUtils
import java.util.concurrent.CompletableFuture

object TestFunctions {
    @JvmStatic
    fun exception() {
        throw Exception("EXCEPTION")
    }

    @JvmStatic
    fun successCf(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("SUCCESS")
    }

    @JvmStatic
    fun failedCf(): CompletableFuture<String> {
        return FutureUtils.failed(Exception("FAILED"))
    }
}