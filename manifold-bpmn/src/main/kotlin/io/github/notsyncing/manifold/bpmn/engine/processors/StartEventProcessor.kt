package io.github.notsyncing.manifold.bpmn.engine.processors

import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionState
import io.github.notsyncing.manifold.bpmn.engine.BpmnNodeProcessor
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine
import io.github.notsyncing.manifold.bpmn.engine.ProcessResult
import org.camunda.bpm.model.bpmn.instance.StartEvent
import java.util.concurrent.CompletableFuture

class StartEventProcessor : BpmnNodeProcessor<StartEvent> {
    override fun process(engine: BpmnProcessEngine<*>, currNode: StartEvent, currExecuteInfo: BpmnNodeExecutionInfo): CompletableFuture<ProcessResult> {
        currExecuteInfo.state = BpmnNodeExecutionState.Executed
        return CompletableFuture.completedFuture(ProcessResult(false))
    }
}