package io.github.notsyncing.manifold.bpmn.engine.processors

import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import io.github.notsyncing.manifold.bpmn.engine.BpmnNodeProcessor
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine
import io.github.notsyncing.manifold.bpmn.engine.ProcessResult
import org.camunda.bpm.model.bpmn.instance.EndEvent
import java.util.concurrent.CompletableFuture

class EndEventProcessor : BpmnNodeProcessor<EndEvent> {
    override fun process(engine: BpmnProcessEngine<*>, currNode: EndEvent, currExecuteInfo: BpmnNodeExecutionInfo): CompletableFuture<ProcessResult> {
        val r = when (engine.lastNodeResults?.size) {
            null, 0 -> null
            1 -> engine.lastNodeResults!!.values.firstOrNull()
            else -> engine.lastNodeResults!!.values.toList()
        }

        try {
            engine.invokeEndHandler(r)
        } catch (e: ClassCastException) {
            engine.invokeEndHandler(null)
        }

        try {
            return CompletableFuture.completedFuture(ProcessResult(true, r))
        } catch (e: ClassCastException) {
            return CompletableFuture.completedFuture(ProcessResult(true, null))
        }
    }
}