package io.github.notsyncing.manifold.integration.cowherd

import io.github.notsyncing.cowherd.api.ApiExecutor
import io.github.notsyncing.manifold.story.vm.StoryLibrary
import io.vertx.core.http.HttpServerRequest
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KCallable
import kotlin.reflect.jvm.reflect

class ManifoldStoryNarrator : ApiExecutor() {
    fun doNothing() {}

    override fun execute(method: KCallable<*>, args: MutableList<Any?>, sessionIdentifier: String?, request: HttpServerRequest?): CompletableFuture<Any?> {
        return StoryLibrary.tell(args[0] as String, sessionIdentifier) as CompletableFuture<Any?>
    }

    override fun getDefaultMethod(): KCallable<*> {
        return this::doNothing
    }
}