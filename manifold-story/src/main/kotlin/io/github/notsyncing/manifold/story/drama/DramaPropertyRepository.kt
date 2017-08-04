package io.github.notsyncing.manifold.story.drama

import java.util.concurrent.CompletableFuture

abstract class DramaPropertyRepository<T>(protected val context: DramaActionContext) {
    init {
        context.registerRepository(this)
    }

    protected val sceneContext get() = context.sceneContext

    abstract fun get(keys: Map<String, Any?>): CompletableFuture<T>

    abstract fun save(model: T): CompletableFuture<Unit>

    open fun destroy(): CompletableFuture<Unit> {
        return CompletableFuture.completedFuture(Unit)
    }
}