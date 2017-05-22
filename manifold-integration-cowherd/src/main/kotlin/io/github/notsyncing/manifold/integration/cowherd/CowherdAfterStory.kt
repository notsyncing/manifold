package io.github.notsyncing.manifold.integration.cowherd

import io.github.notsyncing.manifold.story.AfterStory
import io.vertx.core.http.HttpServerRequest
import java.util.concurrent.CompletableFuture

abstract class CowherdAfterStory : AfterStory() {
    open fun continuation(currResult: Any?, request: HttpServerRequest?, sessionIdentifier: String?): CompletableFuture<Any?> {
        return CompletableFuture.completedFuture(currResult)
    }
}