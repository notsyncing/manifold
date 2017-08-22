package io.github.notsyncing.manifold.story.drama

import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import jdk.nashorn.api.scripting.ScriptObjectMirror
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import javax.script.Invocable
import javax.script.ScriptEngine

class DramaActionContext(private val engine: ScriptEngine,
                         private val scene: DramaScene) {
    private val repoList = mutableListOf<DramaPropertyRepository<*>>()
    private val engineInvocable = engine as Invocable

    val sceneContext get() = scene.context
    val m get() = scene.m

    fun registerRepository(repo: DramaPropertyRepository<*>) {
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

    fun emit(event: String, data: ScriptObjectMirror) {
        val json = engine.get("JSON")
        val str = engineInvocable.invokeMethod(json, "stringify", data) as String

        emit(event, str)
    }
}