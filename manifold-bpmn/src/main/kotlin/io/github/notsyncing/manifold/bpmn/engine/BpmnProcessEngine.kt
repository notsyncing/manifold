package io.github.notsyncing.manifold.bpmn.engine

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionState
import io.github.notsyncing.manifold.bpmn.BpmnScene
import io.github.notsyncing.manifold.bpmn.WaitStrategy
import io.github.notsyncing.manifold.bpmn.engine.processors.EndEventProcessor
import io.github.notsyncing.manifold.bpmn.engine.processors.ExclusiveGatewayProcessor
import io.github.notsyncing.manifold.bpmn.engine.processors.StartEventProcessor
import io.github.notsyncing.manifold.bpmn.engine.processors.TaskProcessor
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.MapContext
import org.camunda.bpm.model.bpmn.BpmnModelInstance
import org.camunda.bpm.model.bpmn.instance.*
import java.io.InvalidObjectException
import java.security.InvalidParameterException
import java.util.concurrent.CompletableFuture
import kotlin.reflect.jvm.javaType

class BpmnProcessEngine<R>(val scene: BpmnScene<*>) {
    companion object {
        const val BPMN_SCENE_EXECUTED_NODES = "manifold.bpmn.node.executed_list"
        const val BPMN_SCENE_PROCESS_NAME = "manifold.bpmn.process.name"

        const val MANIFOLD_BPMN_XML_NS = "http://notsyncing.github.io/manifold/bpmn"

        private val processors = mutableMapOf<Class<FlowElement>, BpmnNodeProcessor<FlowNode>>()

        init {
            registerProcessor<StartEvent>(StartEventProcessor())
            registerProcessor<EndEvent>(EndEventProcessor())
            registerProcessor<ExclusiveGateway>(ExclusiveGatewayProcessor())
            registerProcessor<Task>(TaskProcessor())
        }

        private inline fun <reified N: FlowElement> registerProcessor(processor: BpmnNodeProcessor<*>) {
            processors[N::class.java as Class<FlowElement>] = processor as BpmnNodeProcessor<FlowNode>
        }
    }

    private var executedNodes = mutableMapOf<String, BpmnNodeExecutionInfo>()
    private val exprEngine = JexlBuilder().create()
    private var exprContext = MapContext()

    var lastNodeResults: Map<String, Any?>? = null
    var currNode: FlowNode? = null

    var endEventHandler: ((R) -> Unit)? = null

    init {
        exprContext.set("scene", scene)
        exprContext.set(WaitStrategy::class.java.simpleName, WaitStrategy::class.java)
    }

    fun serializeInto(o: JSONObject): JSONObject {
        return o.fluentPut(BPMN_SCENE_EXECUTED_NODES, executedNodes)
                .fluentPut(BPMN_SCENE_PROCESS_NAME, scene.bpmnProcessName)
    }

    fun deserializeFrom(o: JSONObject) {
        executedNodes = JSON.parseObject(o.getJSONObject(BPMN_SCENE_EXECUTED_NODES).toString(), this::executedNodes.returnType.javaType)
    }

    fun hasNodeExpression(node: FlowElement): Boolean {
        return node.getAttributeValueNs(BpmnProcessEngine.MANIFOLD_BPMN_XML_NS, "expression") != null
    }

    fun executeNodeExpression(node: FlowElement) = future<Any?> {
        val exprStr = node.getAttributeValueNs(MANIFOLD_BPMN_XML_NS, "expression")

        if (exprStr == null) {
            throw InvalidParameterException("Node $this has no expression attribute!")
        }

        val expr = exprEngine.createExpression(exprStr)
        val o = expr.evaluate(exprContext)

        if (o is CompletableFuture<*>) {
            o.await()
        } else {
            o
        }
    }

    fun updateLastNodeResults(results: Map<String, Any?>?) {
        lastNodeResults = results

        if (results?.size == 0) {
            exprContext.set("result", null)
        } else if (results?.size != null) {
            exprContext.set("result", results.entries.first().value)
            exprContext.set("results", results)
        }
    }

    fun updateLastNodeResult(result: Any?)  {
        updateLastNodeResults(mapOf("RESULT" to result))
    }

    fun process(bpmn: BpmnModelInstance) = future<R> {
        val bpmnProcessName = scene.bpmnProcessName

        val starts = bpmn.getModelElementsByType(StartEvent::class.java)

        if (starts.isEmpty()) {
            throw InvalidObjectException("BPMN diagram $bpmnProcessName has no start element!")
        }

        val start = starts.first()
        currNode = start

        while (currNode != null) {
            val executedInfo = executedNodes[currNode!!.id]

            if (executedInfo?.state == BpmnNodeExecutionState.Executed) {
                if (executedInfo.nextNodeId != null) {
                    currNode = currNode!!.outgoing.first { it.target.id == executedInfo.nextNodeId }.target
                } else {
                    currNode = currNode!!.outgoing.first().target
                }

                continue
            }

            if (!executedNodes.containsKey(currNode!!.id)) {
                executedNodes[currNode!!.id] = BpmnNodeExecutionInfo()
            }

            val currExecuteInfo = executedNodes[currNode!!.id]!!
            val processor = processors.entries.firstOrNull { it.key.isAssignableFrom(currNode!!::class.java) }?.value

            if (processor == null) {
                throw UnsupportedOperationException("BPMN diagram $bpmnProcessName contains an unsupported node: $currNode")
            }

            val processResult = processor.process(this, currNode as FlowNode, currExecuteInfo).await()

            if (processResult.needReturn) {
                return@future processResult.result as R
            }

            if (currExecuteInfo.nextNodeId == null) {
                currNode = currNode?.outgoing?.first()?.target
            } else {
                currNode = bpmn.getModelElementById(currExecuteInfo.nextNodeId)

                if (currNode == null) {
                    throw InvalidParameterException("Node id ${currExecuteInfo.nextNodeId} not found in diagram $bpmnProcessName")
                }
            }
        }

        null as R
    }

    fun invokeEndHandler(r: Any?) {
        endEventHandler?.invoke(r as R)
    }
}