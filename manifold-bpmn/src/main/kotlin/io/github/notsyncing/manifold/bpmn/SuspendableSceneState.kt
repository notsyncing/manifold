package io.github.notsyncing.manifold.bpmn

import com.alibaba.fastjson.JSONObject

class SuspendableSceneState {
    var sceneClassFullName: String = ""
    var sceneState: JSONObject = JSONObject()
    var sceneSessionId: String? = null
    var sceneTaskId: String? = null
    var awaitStrategy = WaitStrategy.And
    var awaitingActions: Map<String, ActionResult> = mapOf()
}