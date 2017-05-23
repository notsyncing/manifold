package io.github.notsyncing.manifold.integration.cowherd

import com.alibaba.fastjson.JSON
import io.github.notsyncing.cowherd.api.ApiExecutor
import io.github.notsyncing.manifold.story.vm.StoryLibrary
import io.github.notsyncing.manifold.suspendable.SuspendableSceneScheduler
import io.vertx.core.http.HttpServerRequest
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter

class ManifoldStoryNarrator : ApiExecutor() {
    fun doNothing() {}

    override fun execute(method: KCallable<*>, args: MutableMap<KParameter, Any?>, sessionIdentifier: String?,
                         request: HttpServerRequest?) = future<Any?> {
        val sceneName = args.values.first() as String
        var (taskId, ending) = StoryLibrary.tell(sceneName, sessionIdentifier).await()

        if (SuspendableSceneScheduler.isTaskDone(taskId)) {
            for (s in StoryLibrary.afterStories(sceneName)) {
                if (!CowherdAfterStory::class.java.isAssignableFrom(s)) {
                    continue
                }

                val afterStory = s.newInstance() as CowherdAfterStory
                ending = afterStory.continuation(ending, request, sessionIdentifier).await()
            }
        }

        JSON.toJSON(ending)
    }

    override fun getDefaultMethod(): KCallable<*> {
        return this::doNothing
    }
}