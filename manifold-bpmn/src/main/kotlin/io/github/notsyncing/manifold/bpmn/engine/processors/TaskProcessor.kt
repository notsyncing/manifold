package io.github.notsyncing.manifold.bpmn.engine.processors

import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionState
import io.github.notsyncing.manifold.bpmn.engine.BpmnNodeProcessor
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine
import io.github.notsyncing.manifold.bpmn.engine.ProcessResult
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.camunda.bpm.model.bpmn.instance.Task

class TaskProcessor : BpmnNodeProcessor<Task> {
    override fun process(engine: BpmnProcessEngine<*>, currNode: Task, currExecuteInfo: BpmnNodeExecutionInfo) = future<ProcessResult> {
        if (currExecuteInfo.state == BpmnNodeExecutionState.Suspended) {
            currExecuteInfo.state = BpmnNodeExecutionState.Executed

            engine.updateLastNodeResults(engine.scene.resumedResults)
            ProcessResult(false)
        } else {
            val o = engine.executeNodeExpression(currNode).await()

            if (engine.scene.suspended) {
                currExecuteInfo.state = BpmnNodeExecutionState.Suspended
                ProcessResult(true, null)
            } else {
                engine.updateLastNodeResult(o)
                ProcessResult(false)
            }
        }
    }
}