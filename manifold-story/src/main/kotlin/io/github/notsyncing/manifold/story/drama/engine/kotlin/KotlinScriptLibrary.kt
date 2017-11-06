package io.github.notsyncing.manifold.story.drama.engine.kotlin

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.hooking.Hook
import io.github.notsyncing.manifold.story.drama.DramaActionContext
import io.github.notsyncing.manifold.story.drama.DramaManager
import java.util.concurrent.CompletableFuture

typealias KotlinScriptAction = (DramaActionContext, JSONObject, JSONObject?) -> CompletableFuture<Any?>

class Role(private val permissionName: String? = null,
           private val permissionType: String? = null) {
    fun on(actionName: String, handler: KotlinScriptAction, isInternal: Boolean) {
        val h = { context: DramaActionContext, params: String, permParams: String? ->
            handler(context, JSON.parseObject(params), if (permParams == null) null else JSON.parseObject(permParams))
        }

        val callable = KotlinScriptDramaEngine.toCallable(h)

        DramaManager.registerAction(actionName, isInternal, permissionName, permissionType, callable,
                DramaManager.currentFile ?: "", null)
    }

    fun on(actionName: String, handler: KotlinScriptAction) {
        on(actionName, handler, false)
    }
}

fun hooking(name: String, hook: Hook<*>) {
    val hookSource = DramaManager.currentFile + "?" + name

    Manifold.hooks.registerHook(name, null, hook.javaClass, hookSource)
}