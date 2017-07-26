package io.github.notsyncing.manifold.bpmn.engine

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionInfo
import io.github.notsyncing.manifold.bpmn.BpmnNodeExecutionState
import io.github.notsyncing.manifold.bpmn.BpmnScene
import io.github.notsyncing.manifold.bpmn.engine.processors.*
import io.github.notsyncing.manifold.suspendable.WaitStrategy
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
        const val BPMN_SCENE_NODE_ID = "manifold.bpmn.node.id"

        const val MANIFOLD_BPMN_XML_NS = "http://notsyncing.github.io/manifold/bpmn"

        private val processors = mutableMapOf<Class<FlowElement>, BpmnNodeProcessor<FlowNode>>()

        init {
            registerProcessor<StartEvent>(StartEventProcessor())
            registerProcessor<EndEvent>(EndEventProcessor())
            registerProcessor<ExclusiveGateway>(ExclusiveGatewayProcessor())
            registerProcessor<Task>(TaskProcessor())
            registerProcessor<ParallelGateway>(ParallelGatewayProcessor())
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
    var currBpmnModel: BpmnModelInstance? = null

    var endEventHandler: ((R) -> Unit)? = null

    init {
        exprContext.set("scene", scene)
        exprContext.set(WaitStrategy::class.java.simpleName, WaitStrategy::class.java)
    }

    fun getNodeExecutionInfo(nodeId: String): BpmnNodeExecutionInfo? {
        return executedNodes[nodeId]
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

    fun getLastNodeResult() = lastNodeResults?.get("RESULT") ?: lastNodeResults?.values?.firstOrNull()

    fun processFromNode(bpmn: BpmnModelInstance, node: FlowNode): CompletableFuture<ProcessResult> = future {
        currBpmnModel = bpmn
        currNode = node

        while (currNode != null) {
            val executedInfo = executedNodes[currNode!!.id]

            if (executedInfo?.state == BpmnNodeExecutionState.Executed) {
                updateLastNodeResult(executedInfo.result)

                if (executedInfo.nextNodeId != null) {
                    currNode = currNode!!.outgoing.first { it.target.id == executedInfo.nextNodeId }.target
                } else if (executedInfo.everyNextNode) {
                    var lastResult: ProcessResult? = null

                    for (n in currNode!!.outgoing) {
                        lastResult = processFromNode(bpmn, n.target).await()
                    }

                    return@future lastResult!!
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
                throw UnsupportedOperationException("BPMN diagram ${scene.bpmnProcessName} contains an unsupported node: $currNode")
            }

            val processResult = processor.process(this@BpmnProcessEngine, currNode as FlowNode, currExecuteInfo).await()
            currExecuteInfo.result = processResult.result

            if (processResult.needReturn) {
                return@future processResult
            }

            if (currExecuteInfo.nextNodeId == null) {
                currNode = currNode?.outgoing?.firstOrNull()?.target
            } else {
                currNode = bpmn.getModelElementById(currExecuteInfo.nextNodeId)

                if (currNode == null) {
                    throw InvalidParameterException("Node id ${currExecuteInfo.nextNodeId} not found in diagram ${scene.bpmnProcessName}")
                }
            }
        }

        ProcessResult(true, getLastNodeResult())
    }

    fun process(bpmn: BpmnModelInstance) = future<R> {
        currBpmnModel = bpmn
        val bpmnProcessName = scene.bpmnProcessName

        val starts = bpmn.getModelElementsByType(StartEvent::class.java)

        if (starts.isEmpty()) {
            throw InvalidObjectException("BPMN diagram $bpmnProcessName has no start element!")
        }

        // TODO: Support multiple [StartEvent]s?
        val start = starts.first()
        processFromNode(bpmn, start).await().result as R
    }

    fun invokeEndHandler(r: Any?) {
        endEventHandler?.invoke(r as R)
    }
}