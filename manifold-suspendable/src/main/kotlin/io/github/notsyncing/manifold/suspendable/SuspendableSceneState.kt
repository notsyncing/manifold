package io.github.notsyncing.manifold.suspendable

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldAction
import java.util.concurrent.TimeUnit

class SuspendableSceneState {
    class AwaitingAction() {
        var waitStrategy = WaitStrategy.And
        var passed = false
        var results = mutableMapOf<String, WaitActionResult>()
        var additionalData = mutableMapOf<String, Any?>()

        constructor(waitStrategy: WaitStrategy, passed: Boolean, results: MutableMap<String, WaitActionResult>) : this() {
            this.waitStrategy = waitStrategy
            this.passed = passed
            this.results = results
        }
    }

    var sceneClassFullName: String = ""
    var sceneState: com.alibaba.fastjson.JSONObject = JSONObject()
    var sceneSessionId: String? = null
    var sceneTaskId: String? = null
    var awaitingActions: MutableList<AwaitingAction> = mutableListOf()
    var timeout: Long = 0
    var timeoutUnit: TimeUnit = TimeUnit.MILLISECONDS

    fun getAwaitingActionResult(actionClassName: String): WaitActionResult? {
        for (aw in awaitingActions) {
            if (aw.results.containsKey(actionClassName)) {
                return aw.results[actionClassName]
            }
        }

        return null
    }

    fun <T: ManifoldAction<*>> getAwaitingActionResult(actionClass: Class<T>): WaitActionResult? {
        return getAwaitingActionResult(actionClass.name)
    }
}