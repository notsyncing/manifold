package io.github.notsyncing.manifold.bpmn.engine.processors

import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionState
import io.github.notsyncing.manifold.bpmn.SuspendableScene.Companion.SUSPENDED
import io.github.notsyncing.manifold.bpmn.engine.BpmnNodeProcessor
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine.Companion.BPMN_SCENE_NODE_ID
import io.github.notsyncing.manifold.bpmn.engine.ProcessResult
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.camunda.bpm.model.bpmn.instance.Task

class TaskProcessor : BpmnNodeProcessor<Task> {
    override fun process(engine: BpmnProcessEngine<*>, currNode: Task, currExecuteInfo: BpmnNodeExecutionInfo) = future<ProcessResult> {
        if (currExecuteInfo.state == BpmnNodeExecutionState.Suspended) {
            currExecuteInfo.state = BpmnNodeExecutionState.Executed

            val results = engine.scene.resumedState?.awaitingActions?.firstOrNull { it.additionalData[BPMN_SCENE_NODE_ID] == currNode.id }
                    ?.results
            val r = results?.values?.firstOrNull()?.result

            engine.updateLastNodeResult(r)
            ProcessResult(false, r)
        } else {
            engine.scene.suspendAdditionalData = mutableMapOf(BPMN_SCENE_NODE_ID to currNode.id)
            val o = engine.executeNodeExpression(currNode).await()

            if (o == SUSPENDED) {
                currExecuteInfo.state = BpmnNodeExecutionState.Suspended
                ProcessResult(true, null)
            } else {
                currExecuteInfo.state = BpmnNodeExecutionState.Executed
                engine.updateLastNodeResult(o)
                ProcessResult(false, o)
            }
        }
    }
}