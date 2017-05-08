package io.github.notsyncing.manifold.bpmn

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine
import io.github.notsyncing.manifold.bpmn.engine.BpmnProcessEngine.Companion.BPMN_SCENE_PROCESS_NAME
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.suspendable.SuspendableScene
import io.github.notsyncing.manifold.suspendable.WaitStrategy
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.camunda.bpm.model.bpmn.Bpmn
import java.util.concurrent.CompletableFuture

open class BpmnScene<R>(var bpmnProcessName: String) : SuspendableScene<R>() {
    @AutoProvide
    lateinit var storageProvider: BpmnStorageProvider

    private val engine = BpmnProcessEngine<R>(this)

    constructor() : this("")

    override fun serialize(): JSONObject {
        return engine.serializeInto(JSONObject())
    }

    override fun deserialize(o: JSONObject) {
        engine.deserializeFrom(o)
        bpmnProcessName = o.getString(BPMN_SCENE_PROCESS_NAME)
    }

    fun awaitFor(waitStrategy: WaitStrategy, actionClassName: String): Any {
        return super.awaitFor(waitStrategy, Class.forName(actionClassName) as Class<ManifoldAction<*>>)
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

    override fun stage() = future<R> {
        super.stage().await()

        val bpmnDiagram = storageProvider.getDiagram(bpmnProcessName).await()
        val bpmn = Bpmn.readModelFromStream(bpmnDiagram.byteInputStream())

        engine.endEventHandler = this::onEndEvent

        engine.process(bpmn).await()
    }

    open protected fun onEndEvent(result: R) {}
}