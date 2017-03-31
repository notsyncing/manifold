package io.github.notsyncing.manifold.bpmn.engine

import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import org.camunda.bpm.model.bpmn.instance.FlowNode
import java.util.concurrent.CompletableFuture

interface BpmnNodeProcessor<N: FlowNode> {
    fun process(engine: BpmnProcessEngine<*>, currNode: N, currExecuteInfo: BpmnNodeExecutionInfo): CompletableFuture<ProcessResult>
}