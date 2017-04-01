package io.github.notsyncing.manifold.bpmn.engine.processors

import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionState
import io.github.notsyncing.manifold.bpmn.engine.BpmnNodeProcessor
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine
import io.github.notsyncing.manifold.bpmn.engine.ProcessResult
import kotlinx.coroutines.experimental.future.future
import org.camunda.bpm.model.bpmn.GatewayDirection
import org.camunda.bpm.model.bpmn.instance.ParallelGateway

class ParallelGatewayProcessor : BpmnNodeProcessor<ParallelGateway> {
    override fun process(engine: BpmnProcessEngine<*>, currNode: ParallelGateway, currExecuteInfo: BpmnNodeExecutionInfo) = future<ProcessResult> {
        val direction = determineGatewayDirection(currNode)

        when (direction) {
            GatewayDirection.Converging -> {

            }

            GatewayDirection.Diverging -> {

            }

            else -> throw UnsupportedOperationException("Unsupported direction $direction in gateway $currNode of diagram ${engine.scene.bpmnProcessName}")
        }

        currExecuteInfo.state = BpmnNodeExecutionState.Executed

        ProcessResult(false)
    }
}