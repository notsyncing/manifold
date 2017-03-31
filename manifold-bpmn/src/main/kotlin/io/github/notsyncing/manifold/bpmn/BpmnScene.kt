package io.github.notsyncing.manifold.bpmn

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import io.github.notsyncing.manifold.di.AutoProvide
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.MapContext
import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.instance.*
import java.io.InvalidObjectException
import java.security.InvalidParameterException
import java.util.concurrent.CompletableFuture
import kotlin.reflect.jvm.javaType

open class BpmnScene<R>(private var bpmnProcessName: String) : SuspendableScene<R>() {
    companion object {
        const val BPMN_SCENE_EXECUTED_NODES = "manifold.bpmn.node.executed_list"
        const val BPMN_SCENE_PROCESS_NAME = "manifold.bpmn.process.name"
        const val BPMN_SCENE_EXPRESSION_CONTEXT = "manifold.bpmn.process.expr.context"

        const val MANIFOLD_BPMN_XML_NS = "http://notsyncing.github.io/manifold/bpmn"
    }

    @AutoProvide
    lateinit var storageProvider: BpmnStorageProvider

    private var executedNodes = mutableMapOf<String, BpmnNodeExecutionInfo>()
    private val exprEngine = JexlBuilder().create()
    private var exprContext = MapContext()
    private var lastNodeResults: Map<String, Any?>? = null

    constructor() : this("")

    init {
        exprContext.set("scene", this)
        exprContext.set(WaitStrategy::class.java.simpleName, WaitStrategy::class.java)
    }

    override fun shouldAccept(state: SuspendableSceneState, actionContext: ActionInterceptorContext): Boolean {
        return true
    }

    override fun serialize(): JSONObject {
        return JSONObject()
                .fluentPut(BPMN_SCENE_EXECUTED_NODES, executedNodes)
                .fluentPut(BPMN_SCENE_PROCESS_NAME, bpmnProcessName)
    }

    override fun deserialize(o: JSONObject) {
        executedNodes = JSON.parseObject(o.getJSONObject(BPMN_SCENE_EXECUTED_NODES).toString(), this::executedNodes.returnType.javaType)
        bpmnProcessName = o.getString(BPMN_SCENE_PROCESS_NAME)
    }

    fun awaitFor(waitStrategy: WaitStrategy, actionClassName: String) {
        super.awaitFor(waitStrategy, Class.forName(actionClassName) as Class<ManifoldAction<*>>)
    }

    fun executeAction(actionClassName: String, vararg args: Any?): CompletableFuture<Any?> {
        val actionClass = Class.forName(actionClassName)
        val action: ManifoldAction<Any?>

        if (args.isEmpty()) {
            action = actionClass.newInstance() as ManifoldAction<Any?>
        } else {
            action = actionClass.constructors[0].newInstance(args) as ManifoldAction<Any?>
        }

        return m(action)
    }

    private fun FlowElement.hasNodeExpression(): Boolean {
        return this.getAttributeValueNs(MANIFOLD_BPMN_XML_NS, "expression") != null
    }

    private fun FlowElement.executeNodeExpression() = future<Any?> {
        val exprStr = this.getAttributeValueNs(MANIFOLD_BPMN_XML_NS, "expression")

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

    private fun updateLastNodeResults(results: Map<String, Any?>?) {
        lastNodeResults = results

        if (results?.size == 0) {
            exprContext.set("result", null)
        } else if (results?.size != null) {
            exprContext.set("result", results.entries.first().value)
            exprContext.set("results", results)
        }
    }

    private fun updateLastNodeResult(result: Any?) {
        updateLastNodeResults(mapOf("RESULT" to result))
    }

    override fun stage() = future<R> {
        super.stage().await()

        val bpmnDiagram = storageProvider.getDiagram(bpmnProcessName).await()
        val bpmn = Bpmn.readModelFromStream(bpmnDiagram.byteInputStream())

        val starts = bpmn.getModelElementsByType(StartEvent::class.java)

        if (starts.isEmpty()) {
            throw InvalidObjectException("BPMN diagram $bpmnProcessName has no start element!")
        }

        val start = starts.first()
        var currNode: FlowNode? = start

        while (currNode != null) {
            val executedInfo = executedNodes[currNode.id]

            if (executedInfo?.state == BpmnNodeExecutionState.Executed) {
                if (executedInfo.nextNodeId != null) {
                    currNode = currNode.outgoing.first { it.target.id == executedInfo.nextNodeId }.target
                } else {
                    currNode = currNode.outgoing.first().target
                }

                continue
            }

            if (!executedNodes.containsKey(currNode.id)) {
                executedNodes[currNode.id] = BpmnNodeExecutionInfo()
            }

            val currExecuteInfo = executedNodes[currNode.id]!!

            when (currNode) {
                is StartEvent -> {
                    currExecuteInfo.state = BpmnNodeExecutionState.Executed
                }

                is Task -> {
                    if (currExecuteInfo.state == BpmnNodeExecutionState.Suspended) {
                        currExecuteInfo.state = BpmnNodeExecutionState.Executed

                        updateLastNodeResults(resumedResults)
                    } else {
                        val o = currNode.executeNodeExpression().await()

                        if (suspended) {
                            currExecuteInfo.state = BpmnNodeExecutionState.Suspended
                            return@future null as R
                        } else {
                            updateLastNodeResult(o)
                        }
                    }
                }

                is ExclusiveGateway -> {
                    if (currNode.hasNodeExpression()) {
                        val o = currNode.executeNodeExpression().await()
                        updateLastNodeResult(o)
                    }

                    val nextRoute = currNode.outgoing
                            .filter {
                                if (it.hasNodeExpression()) {
                                    it.executeNodeExpression().await() as Boolean
                                } else {
                                    true
                                }
                            }
                            .firstOrNull()

                    if (nextRoute == null) {
                        throw InvalidParameterException("No branch of exclusive gateway $currNode is satisified in diagram $bpmnProcessName")
                    }

                    currExecuteInfo.nextNodeId = nextRoute.target.id
                    currExecuteInfo.state = BpmnNodeExecutionState.Executed
                }

                is EndEvent -> {
                    val r = lastNodeResults?.values?.firstOrNull() as? R as R

                    try {
                        onEndEvent(r)
                    } catch (e: ClassCastException) {
                        onEndEvent(null as R)
                    }

                    try {
                        return@future r
                    } catch (e: ClassCastException) {
                        return@future null as R
                    }
                }

                else -> throw UnsupportedOperationException("BPMN diagram $bpmnProcessName contains an unsupported node: $currNode")
            }

            currNode = bpmn.getModelElementById(currExecuteInfo.nextNodeId) ?: currNode.outgoing.first().target
        }

        null as R
    }

    open protected fun onEndEvent(result: R) {}
}