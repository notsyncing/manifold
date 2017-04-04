package io.github.notsyncing.manifold.bpmn.tests.toys

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import io.github.notsyncing.manifold.bpmn.SuspendableScene
import io.github.notsyncing.manifold.bpmn.SuspendableSceneState
import io.github.notsyncing.manifold.bpmn.WaitStrategy
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future

class TestAwaitBothSuspendScene : SuspendableScene<String>() {
    companion object {
        var finalResult: String? = null
    }

    private var label: Int = 0

    override fun shouldAccept(state: SuspendableSceneState, actionContext: ActionInterceptorContext): Boolean {
        return true
    }

    override fun serialize(): JSONObject {
        return JSONObject()
                .fluentPut("label", label)
    }

    override fun deserialize(o: JSONObject) {
        label = o.getInteger("label")
    }

    override fun stage() = future<String> {
        super.stage().await()

        when (label) {
            0 -> {
                label = 1
                awaitFor(WaitStrategy.And, TestAction1::class.java)
                awaitFor(WaitStrategy.And, TestAction2::class.java)
                finalResult = ""
            }

            1 -> {
                finalResult = resumedState?.awaitingActions?.flatMap { it.results.values }?.map{ it.result }?.joinToString(separator = " ")
            }

            else -> {
                finalResult = "BAD"
            }
        }

        finalResult ?: "NULL"
    }
}