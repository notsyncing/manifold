package io.github.notsyncing.manifold.bpmn.tests.toys

import io.github.notsyncing.manifold.bpmn.BpmnScene

class SimpleBpmnScene(name: String) : BpmnScene<String?>(name) {
    companion object {
        var result: String? = null

        fun reset() {
            result = null
        }
    }

    constructor() : this("")

    override fun onEndEvent(result: String?) {
        SimpleBpmnScene.result = result ?: "<NULL>"
    }
}