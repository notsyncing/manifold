package io.github.notsyncing.manifold.integration.cowherd

import io.github.notsyncing.cowherd.api.ApiExecutor
import io.github.notsyncing.cowherd.models.ActionContext
import io.github.notsyncing.cowherd.models.UploadFileInfo
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.action.describe.DataPolicy
import io.github.notsyncing.manifold.story.StoryLibrary
import io.github.notsyncing.manifold.utils.removeIf
import io.vertx.core.http.HttpMethod
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

class ManifoldSceneApiExecutor(private val sceneClass: Class<ManifoldScene<*>>) : ApiExecutor() {
    companion object {
        private val sceneConstructors = ConcurrentHashMap<Class<ManifoldScene<*>>, KCallable<*>>()

        const val DATA_POLICY = "manifold.scene.api.data_policy"
        const val REQUEST_OBJECT = "manifold.scene.api.request_object"
        const val ALL_UPLOADS = "manifold.scene.api.all_uploads"

        fun removeFromCacheIf(predicate: (Class<ManifoldScene<*>>) -> Boolean) {
            sceneConstructors.removeIf { (clazz, _) -> predicate(clazz) }
        }

        fun removeFromCache(sceneClass: Class<ManifoldScene<*>>) {
            sceneConstructors.remove(sceneClass)
        }

        fun reset() {
            sceneConstructors.clear()
        }
    }

    private fun httpMethodToDataPolicy(httpMethod: HttpMethod?): DataPolicy {
        return when (httpMethod) {
            HttpMethod.GET -> DataPolicy.Get
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE -> DataPolicy.Modify
            else -> DataPolicy.Unknown
        }
    }

    override fun execute(method: KCallable<*>, args: MutableMap<KParameter, Any?>, sessionIdentifier: String?,
                         context: ActionContext, uploads: List<UploadFileInfo>?) = future {
        context.config.isEnumReturnsString = true

        val inst = method.callBy(args) as ManifoldScene<*>

        inst.context.additionalData[DATA_POLICY] = httpMethodToDataPolicy(context.request.method())
        inst.context.additionalData[REQUEST_OBJECT] = context.request
        inst.context.additionalData[ALL_UPLOADS] = uploads

        var ending = Manifold.run(inst, sessionIdentifier).await()
        val sceneName = inst.javaClass.getAnnotation(SceneMetadata::class.java)?.value

        if (sceneName != null) {
            for (s in StoryLibrary.afterStories(sceneName)) {
                if (!CowherdAfterStory::class.java.isAssignableFrom(s)) {
                    continue
                }

                val afterStory = s.newInstance() as CowherdAfterStory
                ending = afterStory.continuation(ending, context.request, sessionIdentifier).await()
            }
        }

        ending
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