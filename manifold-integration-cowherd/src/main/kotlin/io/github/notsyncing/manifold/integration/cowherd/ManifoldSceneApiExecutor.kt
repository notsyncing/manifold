package io.github.notsyncing.manifold.integration.cowherd

import io.github.notsyncing.cowherd.api.ApiExecutor
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.describe.DataPolicy
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

class ManifoldSceneApiExecutor(private val sceneClass: Class<ManifoldScene<*>>) : ApiExecutor() {
    companion object {
        private val sceneConstructors = mutableMapOf<Class<ManifoldScene<*>>, KCallable<*>>()

        const val DATA_POLICY = "manifold.scene.api.data_policy"
        const val REQUEST_OBJECT = "manifold.scene.api.request_object"
    }

    private fun httpMethodToDataPolicy(httpMethod: HttpMethod?): DataPolicy {
        return when (httpMethod) {
            HttpMethod.GET -> DataPolicy.Get
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE -> DataPolicy.Modify
            else -> DataPolicy.Unknown
        }
    }

    override fun execute(method: KCallable<*>, args: MutableMap<KParameter, Any?>, sessionIdentifier: String?, request: HttpServerRequest?): CompletableFuture<Any?> {
        val inst = method.callBy(args) as ManifoldScene<*>

        inst.context.additionalData[DATA_POLICY] = httpMethodToDataPolicy(request?.method())
        inst.context.additionalData[REQUEST_OBJECT] = request

        return Manifold.run(inst, sessionIdentifier) as CompletableFuture<Any?>
    }

    override fun getDefaultMethod(): KCallable<*> {
        var f = sceneConstructors[sceneClass]

        if (f == null) {
            f = sceneClass.kotlin.primaryConstructor

            if (f == null) {
                throw NoSuchMethodException("Scene $sceneClass has no primary constructor!")
            }

            sceneConstructors[sceneClass] = f
        }

        return f
    }
}