package io.github.notsyncing.manifold.integration.cowherd

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import kotlinx.coroutines.experimental.future.future
import java.security.InvalidParameterException
import java.util.concurrent.CompletableFuture

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


    }
}