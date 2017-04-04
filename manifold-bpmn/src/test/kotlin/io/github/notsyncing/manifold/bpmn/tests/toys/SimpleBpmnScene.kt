package io.github.notsyncing.manifold.bpmn.tests.toys

import io.github.notsyncing.manifold.bpmn.BpmnScene

class SimpleBpmnScene(name: String) : BpmnScene<Any?>(name) {
    companion object {
        var result: Any? = null

        fun reset() {
            result = null
        }
    }

    constructor() : this("")

    override fun onEndEvent(result: Any?) {
        SimpleBpmnScene.result = result ?: "<NULL>"
    }
}