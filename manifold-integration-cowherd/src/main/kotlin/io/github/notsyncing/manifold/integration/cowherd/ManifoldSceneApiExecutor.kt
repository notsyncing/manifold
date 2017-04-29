package io.github.notsyncing.manifold.integration.cowherd

import io.github.notsyncing.cowherd.api.ApiExecutor
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KCallable

class ManifoldSceneApiExecutor(private val sceneClass: Class<ManifoldScene<*>>) : ApiExecutor() {
    companion object {
        private val sceneConstructors = mutableMapOf<Class<ManifoldScene<*>>, KCallable<*>>()
    }

    override fun execute(method: KCallable<*>, args: MutableList<Any?>, sessionIdentifier: String?): CompletableFuture<Any?> {
        val inst = method.call(*args.toTypedArray()) as ManifoldScene<*>
        return Manifold.run(inst, sessionIdentifier) as CompletableFuture<Any?>
    }

    override fun getDefaultMethod(): KCallable<*> {
        var f = sceneConstructors[sceneClass]

        if (f == null) {
            f = sceneClass.kotlin.constructors.firstOrNull { it.parameters.isEmpty() }

            if (f == null) {
                throw NoSuchMethodException("Scene $sceneClass has no parameter-less constructor!")
            }

            sceneConstructors[sceneClass] = f
        }

        return f
    }
}