package io.github.notsyncing.manifold.bpmn

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import io.github.notsyncing.manifold.di.AutoProvide
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.MapContext
import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.instance.EndEvent
import org.camunda.bpm.model.bpmn.instance.FlowNode
import org.camunda.bpm.model.bpmn.instance.StartEvent
import org.camunda.bpm.model.bpmn.instance.Task
import java.io.InvalidObjectException

open class BpmnScene<R>(private var bpmnProcessName: String) : SuspendableScene<R>() {
    companion object {
        const val BPMN_SCENE_EXECUTED_NODES = "manifold.bpmn.node.executed_list"
        const val BPMN_SCENE_PROCESS_NAME = "manifold.bpmn.process.name"

        const val MANIFOLD_BPMN_XML_NS = "http://notsyncing.github.io/manifold/bpmn"
    }

    @AutoProvide
    lateinit var storageProvider: BpmnStorageProvider

    private var executedNodes = mutableMapOf<String, BpmnNodeExecutionState>()

    constructor() : this("")

    override fun shouldAccept(state: SuspendableSceneState, actionContext: ActionInterceptorContext): Boolean {
        return true
    }

    override fun serialize(): JSONObject {
        return JSONObject()
                .fluentPut(BPMN_SCENE_EXECUTED_NODES, executedNodes)
                .fluentPut(BPMN_SCENE_PROCESS_NAME, bpmnProcessName)
    }

    override fun deserialize(o: JSONObject) {
        executedNodes = o.getJSONObject(BPMN_SCENE_EXECUTED_NODES)
                .mapValues { BpmnNodeExecutionState.valueOf(it.value as String) }
                .toMutableMap()

        bpmnProcessName = o.getString(BPMN_SCENE_PROCESS_NAME)
    }

    fun awaitFor(waitStrategy: WaitStrategy, actionClassName: String) {
        super.awaitFor(waitStrategy, Class.forName(actionClassName) as Class<ManifoldAction<*>>)
    }

    override fun stage() = future<R> {
        super.stage().await()

        val bpmnDiagram = storageProvider.getDiagram(bpmnProcessName).await()
        val bpmn = Bpmn.readModelFromStream(bpmnDiagram.byteInputStream())

        val starts = bpmn.getModelElementsByType(StartEvent::class.java)

        if (starts.isEmpty()) {
            throw InvalidObjectException("BPMN diagram $bpmnProcessName has no start element!")
        }

        val exprEngine = JexlBuilder().create()
        val exprContext = MapContext()
        exprContext.set("scene", this)
        exprContext.set(WaitStrategy::class.java.simpleName, WaitStrategy::class.java)

        val start = starts.first()
        var currNode: FlowNode? = start
        var lastNodeResults: Map<String, Any?>? = null

        while (currNode != null) {
            if (executedNodes[currNode.id] == BpmnNodeExecutionState.Executed) {
                currNode = currNode.outgoing.first().target
                continue
            }

            when (currNode) {
                is StartEvent -> {
                    executedNodes.put(currNode.id, BpmnNodeExecutionState.Executed)
                }

                is Task -> {
                    if (executedNodes[currNode.id] == BpmnNodeExecutionState.Suspended) {
                        executedNodes[currNode.id] = BpmnNodeExecutionState.Executed

                        lastNodeResults = resumedResults
                    } else {
                        val exprStr = currNode.getAttributeValueNs(MANIFOLD_BPMN_XML_NS, "expression")
                        val expr = exprEngine.createExpression(exprStr)
                        val o = expr.evaluate(exprContext)

                        if (suspended) {
                            executedNodes[currNode.id] = BpmnNodeExecutionState.Suspended
                            return@future null as R
                        } else {
                            lastNodeResults = mapOf("RESULT" to o)
                        }
                    }
                }

                is EndEvent -> {
                    val r = lastNodeResults?.values?.firstOrNull() as R
                    onEndEvent(r)
                    return@future r
                }

                else -> throw UnsupportedOperationException("BPMN diagram $bpmnProcessName contains an unsupported node: $currNode")
            }

            currNode = currNode.outgoing.first().target
        }

        null as R
    }

    open protected fun onEndEvent(result: R) {}
}