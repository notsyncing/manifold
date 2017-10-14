package io.github.notsyncing.manifold.story.drama

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.story.drama.engine.DramaEngine
import io.github.notsyncing.manifold.utils.FutureUtils
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

class DramaActionContext(private val engine: DramaEngine,
                         private val scene: DramaScene,
                         val domain: String? = null) {
    private val repoList = mutableListOf<DramaPropertyRepository>()

    val sceneContext get() = scene.context
    val m get() = scene.m

    val uploads get() = scene.context.additionalData["manifold.scene.api.all_uploads"]
    val uploadMap get() = scene.context.additionalData["manifold.scene.api.all_uploads.map"]

    val role get() = scene.context.role

    fun registerRepository(repo: DramaPropertyRepository) {
        repoList.add(repo)
    }

    fun destroy() = future {
        repoList.forEach { it.destroy().await() }
    }

    fun useTransaction() {
        scene.useTransaction()
    }

    fun splitTransaction() {
        scene.splitTransaction()
    }

    fun emit(event: String, data: String) {
        scene.eventNode?.broadcastToAnyInGroups(ManifoldEvent(event, data))
    }

    fun emit(event: String, data: Any?) {
        val json = engine.get("JSON")
        val str = engine.invoke(json, "stringify", data) as String

        emit(event, str)
    }

    fun fail(message: String): CompletableFuture<Any?> {
        return FutureUtils.failed(DramaExecutionException(message))
    }

    fun m(actionClass: Class<ManifoldAction<*>>, params: Map<String, Any?>): CompletableFuture<Any?> {
        val kotlinClass = actionClass.kotlin
        val constructor = kotlinClass.primaryConstructor ?: return FutureUtils.failed(RuntimeException("Action $actionClass has no primary constructor!"))
        val finalParams = mutableMapOf<KParameter, Any?>()

        for (p in constructor.parameters) {
            finalParams[p] = params[p.name]
        }

        return m.invoke(constructor.callBy(finalParams)) as CompletableFuture<Any?>
    }

    fun hook(name: String, inputValue: Any?): CompletableFuture<Any?> {
        return Manifold.hooks.runHooks(name, domain, inputValue)
    }
}