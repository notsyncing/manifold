package io.github.notsyncing.manifold.bpmn

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldAction

class SuspendableSceneState {
    class AwaitingAction() {
        var waitStrategy = WaitStrategy.And
        var passed = false
        var results = mutableMapOf<String, ActionResult>()
        var additionalData = mutableMapOf<String, Any?>()

        constructor(waitStrategy: WaitStrategy, passed: Boolean, results: MutableMap<String, ActionResult>) : this() {
            this.waitStrategy = waitStrategy
            this.passed = passed
            this.results = results
        }
    }

    var sceneClassFullName: String = ""
    var sceneState: JSONObject = JSONObject()
    var sceneSessionId: String? = null
    var sceneTaskId: String? = null
    var awaitingActions: MutableList<AwaitingAction> = mutableListOf()

    fun getAwaitingActionResult(actionClassName: String): ActionResult? {
        for (aw in awaitingActions) {
            if (aw.results.containsKey(actionClassName)) {
                return aw.results[actionClassName]
            }
        }

        return null
    }

    fun <T: ManifoldAction<*>> getAwaitingActionResult(actionClass: Class<T>): ActionResult? {
        return getAwaitingActionResult(actionClass.name)
    }
}