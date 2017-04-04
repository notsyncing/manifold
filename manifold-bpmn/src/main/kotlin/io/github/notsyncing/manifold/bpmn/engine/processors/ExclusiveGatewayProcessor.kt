package io.github.notsyncing.manifold.bpmn.engine.processors

import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionState
import io.github.notsyncing.manifold.bpmn.engine.BpmnNodeProcessor
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine
import io.github.notsyncing.manifold.bpmn.engine.ProcessResult
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.camunda.bpm.model.bpmn.GatewayDirection
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway
import java.security.InvalidParameterException

class ExclusiveGatewayProcessor : BpmnNodeProcessor<ExclusiveGateway> {
    override fun process(engine: BpmnProcessEngine<*>, currNode: ExclusiveGateway, currExecuteInfo: BpmnNodeExecutionInfo) = future<ProcessResult> {
        var o: Any? = null

        if (engine.hasNodeExpression(currNode)) {
            o = engine.executeNodeExpression(currNode).await()
            engine.updateLastNodeResult(o)
        }

        val direction = determineGatewayDirection(currNode)

        when (direction) {
            GatewayDirection.Converging -> {
                currExecuteInfo.nextNodeId = currNode.outgoing.first().target.id
            }

            GatewayDirection.Diverging -> {
                val nextRoute = currNode.outgoing
                        .filter { engine.executeNodeExpression(it).await() as Boolean }
                        .firstOrNull()

                if (nextRoute == null) {
                    throw InvalidParameterException("No branch of exclusive gateway $currNode is satisified in diagram ${engine.scene.bpmnProcessName}")
                }

                currExecuteInfo.nextNodeId = nextRoute.target.id
            }

            else -> throw UnsupportedOperationException("Unsupported direction $direction in gateway $currNode of diagram ${engine.scene.bpmnProcessName}")
        }

        currExecuteInfo.state = BpmnNodeExecutionState.Executed

        ProcessResult(false, o)
    }
}