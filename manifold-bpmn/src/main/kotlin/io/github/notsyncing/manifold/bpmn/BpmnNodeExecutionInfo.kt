package io.github.notsyncing.manifold.bpmn

class BpmnNodeExecutionInfo() {
    var state = BpmnNodeExecutionState.NotExecuted
    var nextNodeId: String? = null
    var everyNextNode = false
    var result: Any? = null

    constructor(state: BpmnNodeExecutionState) : this() {
        this.state = state
    }
}