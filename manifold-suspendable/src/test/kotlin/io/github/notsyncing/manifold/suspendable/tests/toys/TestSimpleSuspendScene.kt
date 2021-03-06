package io.github.notsyncing.manifold.suspendable.tests.toys

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.suspendable.SuspendableScene
import io.github.notsyncing.manifold.suspendable.WaitStrategy
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future

class TestSimpleSuspendScene : SuspendableScene<String>() {
    companion object {
        var finalResult: String? = null
    }

    private var label: Int = 0

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
                finalResult = ""
            }

            1 -> {
                finalResult = resumedState?.awaitingActions?.get(0)?.results?.values?.firstOrNull()?.result?.toString()
            }

            else -> {
                finalResult = "BAD"
            }
        }

        finalResult ?: "NULL"
    }
}