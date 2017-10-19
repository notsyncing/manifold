package io.github.notsyncing.manifold.story.drama

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.storage.ManifoldStorage
import io.github.notsyncing.manifold.utils.DependencyProviderUtils
import java.util.concurrent.CompletableFuture

abstract class DramaPropertyRepository(protected val context: DramaActionContext) {
    init {
        context.registerRepository(this)

        DependencyProviderUtils.autoProvideProperties(this, this::provideDependency)
    }

    protected val sceneContext get() = context.sceneContext

    private fun provideDependency(t: Class<*>): Any? {
        if (ManifoldStorage::class.java.isAssignableFrom(t)) {
            val o = Manifold.dependencyProvider?.get(t)

            if (o != null) {
                (o as ManifoldStorage<*>).sceneContext = sceneContext
            }

            return o
        } else {
            return Manifold.dependencyProvider?.get(t)
        }
    }

    open fun destroy(): CompletableFuture<Unit> {
        return CompletableFuture.completedFuture(Unit)
    }
}