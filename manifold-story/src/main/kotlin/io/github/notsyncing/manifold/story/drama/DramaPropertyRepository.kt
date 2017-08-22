package io.github.notsyncing.manifold.story.drama

import io.github.notsyncing.manifold.utils.DependencyProviderUtils
import java.util.concurrent.CompletableFuture

abstract class DramaPropertyRepository<T>(protected val context: DramaActionContext) {
    init {
        context.registerRepository(this)

        DependencyProviderUtils.autoProvideProperties(this)
    }

    protected val sceneContext get() = context.sceneContext

    abstract fun get(keys: Map<String, Any?>): CompletableFuture<T?>

    abstract fun save(model: T): CompletableFuture<Any?>

    open fun destroy(): CompletableFuture<Unit> {
        return CompletableFuture.completedFuture(Unit)
    }
}