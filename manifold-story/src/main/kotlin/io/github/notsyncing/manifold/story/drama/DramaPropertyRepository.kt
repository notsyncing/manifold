package io.github.notsyncing.manifold.story.drama

import io.github.notsyncing.manifold.utils.DependencyProviderUtils
import java.util.concurrent.CompletableFuture

abstract class DramaPropertyRepository(protected val context: DramaActionContext) {
    init {
        context.registerRepository(this)

        DependencyProviderUtils.autoProvideProperties(this)
    }

    protected val sceneContext get() = context.sceneContext

    open fun destroy(): CompletableFuture<Unit> {
        return CompletableFuture.completedFuture(Unit)
    }
}