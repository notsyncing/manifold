package io.github.notsyncing.manifold.integration.cowherd

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.cowherd.api.CowherdApiUtils
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ActionMetadata
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.describe.DataPolicy
import io.github.notsyncing.manifold.story.vm.StoryLibrary
import io.github.notsyncing.manifold.suspendable.SuspendableSceneScheduler
import io.vertx.core.http.HttpServerRequest
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.security.InvalidParameterException
import kotlin.reflect.full.primaryConstructor

class ActionPortalScene(private val actionName: String,
                        private val taskId: String,
                        private val parameters: JSONObject) : ManifoldScene<Any?>() {
    constructor() : this("", "", JSONObject())

    override fun stage() = future<Any?> {
        val actionClass = Manifold.actionMetadata[actionName]

        if (actionClass == null) {
            throw ClassNotFoundException("Action $actionName not found!")
        }

        if (taskId.isBlank()) {
            throw InvalidParameterException("You must specify a task id to call action $actionName")
        }

        val currentDataPolicy = context.additionalData[ManifoldSceneApiExecutor.DATA_POLICY] as DataPolicy

        if (actionClass.getAnnotation(ActionMetadata::class.java).dataPolicy != currentDataPolicy) {
            throw IllegalAccessException("Acion $actionName does not like to be called with $currentDataPolicy")
        }

        val actionConstructor = actionClass.kotlin.primaryConstructor!!
        val params = CowherdApiUtils.expandJsonToMethodParameters(actionConstructor, parameters, null)

        m(actionConstructor.callBy(params)).await()

        val (done, r) = SuspendableSceneScheduler.taskStep(taskId).await()
        var ending = r

        if (done) {
            val sceneName = SuspendableSceneScheduler.getTaskSceneName(taskId)

            if (sceneName != null) {
                val request = context.additionalData[ManifoldSceneApiExecutor.REQUEST_OBJECT] as HttpServerRequest

                for (s in StoryLibrary.afterStories(sceneName)) {
                    if (!CowherdAfterStory::class.java.isAssignableFrom(s)) {
                        continue
                    }

                    val afterStory = s.newInstance() as CowherdAfterStory
                    ending = afterStory.continuation(ending, request, sessionIdentifier).await()
                }
            }
        }

        JSON.toJSON(r)
    }
}