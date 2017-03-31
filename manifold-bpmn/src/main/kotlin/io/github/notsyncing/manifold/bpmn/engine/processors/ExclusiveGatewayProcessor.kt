package io.github.notsyncing.manifold.bpmn.engine.processors

import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionState
import io.github.notsyncing.manifold.bpmn.engine.BpmnNodeProcessor
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine
import io.github.notsyncing.manifold.bpmn.engine.ProcessResult
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway
import java.security.InvalidParameterException

class ExclusiveGatewayProcessor : BpmnNodeProcessor<ExclusiveGateway> {
    override fun process(engine: BpmnProcessEngine<*>, currNode: ExclusiveGateway, currExecuteInfo: BpmnNodeExecutionInfo) = future<ProcessResult> {
        if (engine.hasNodeExpression(currNode)) {
            val o = engine.executeNodeExpression(currNode).await()
            engine.updateLastNodeResult(o)
        }

        val nextRoute = currNode.outgoing
                .filter {
                    if (engine.hasNodeExpression(it)) {
                        engine.executeNodeExpression(it).await() as Boolean
                    } else {
                        true
                    }
                }
                .firstOrNull()

        if (nextRoute == null) {
            throw InvalidParameterException("No branch of exclusive gateway $currNode is satisified in diagram ${engine.scene.bpmnProcessName}")
        }

        currExecuteInfo.nextNodeId = nextRoute.target.id
        currExecuteInfo.state = BpmnNodeExecutionState.Executed

        ProcessResult(false)
    }
}