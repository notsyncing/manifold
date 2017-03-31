package io.github.notsyncing.manifold.bpmn.engine

import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import org.camunda.bpm.model.bpmn.GatewayDirection
import org.camunda.bpm.model.bpmn.instance.FlowNode
import org.camunda.bpm.model.bpmn.instance.Gateway
import java.util.concurrent.CompletableFuture

interface BpmnNodeProcessor<N: FlowNode> {
    fun determineGatewayDirection(node: Gateway): GatewayDirection {
        if ((node.incoming.size == 1) && (node.outgoing.size > 1)) {
            node.gatewayDirection = GatewayDirection.Diverging
        } else if ((node.incoming.size > 1) && (node.outgoing.size == 1)) {
            node.gatewayDirection = GatewayDirection.Converging
        } else {
            node.gatewayDirection = GatewayDirection.Unspecified
        }

        return node.gatewayDirection
    }

    fun process(engine: BpmnProcessEngine<*>, currNode: N, currExecuteInfo: BpmnNodeExecutionInfo): CompletableFuture<ProcessResult>
}