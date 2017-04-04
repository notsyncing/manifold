package io.github.notsyncing.manifold.bpmn.engine.processors

import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionState
import io.github.notsyncing.manifold.bpmn.SuspendableScene.Companion.SUSPENDED
import io.github.notsyncing.manifold.bpmn.engine.BpmnNodeProcessor
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine
import io.github.notsyncing.manifold.bpmn.engine.ProcessResult
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.camunda.bpm.model.bpmn.GatewayDirection
import org.camunda.bpm.model.bpmn.instance.ParallelGateway
import java.security.InvalidParameterException

class ParallelGatewayProcessor : BpmnNodeProcessor<ParallelGateway> {
    override fun process(engine: BpmnProcessEngine<*>, currNode: ParallelGateway, currExecuteInfo: BpmnNodeExecutionInfo) = future<ProcessResult> {
        val direction = determineGatewayDirection(currNode)

        when (direction) {
            GatewayDirection.Converging -> {
                val incompletePreviousNodes = currNode.incoming.map { it.source }
                        .map { Pair(it.id, engine.getNodeExecutionInfo(it.id)) }
                        .filter { (_, info) -> (info == null) || (info.state != BpmnNodeExecutionState.Executed) }

                if (incompletePreviousNodes.isNotEmpty()) {
                    if (incompletePreviousNodes.any { (_, info) -> info == null }) {
                        return@future ProcessResult(true)
                    }

                    if (incompletePreviousNodes.any { (_, info) -> info?.state != BpmnNodeExecutionState.Suspended }) {
                        throw InvalidParameterException("Converging parallel gateway cannot wait for a non-suspended node!")
                    }

                    ProcessResult(true, SUSPENDED)
                } else {
                    currExecuteInfo.state = BpmnNodeExecutionState.Executed
                    val data = currNode.incoming.map { Pair(it.source.id, engine.getNodeExecutionInfo(it.source.id)?.result) }
                            .associateBy({ (id, _) -> id }, { (_, data) -> data })
                    engine.updateLastNodeResults(data)

                    ProcessResult(false, data.values.toList())
                }
            }

            GatewayDirection.Diverging -> {
                currExecuteInfo.state = BpmnNodeExecutionState.Executed
                currExecuteInfo.everyNextNode = true

                val r = currNode.outgoing.map { it.target }
                        .map { engine.processFromNode(engine.currBpmnModel!!, it).await() }
                        .map { it.result }

                ProcessResult(r.contains(SUSPENDED), r)
            }

            else -> throw UnsupportedOperationException("Unsupported direction $direction in gateway $currNode of diagram ${engine.scene.bpmnProcessName}")
        }
    }
}