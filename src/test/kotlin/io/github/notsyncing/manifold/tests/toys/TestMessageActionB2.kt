package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.ManifoldRunnerContext
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestMessageActionB2 : ManifoldAction<String, ManifoldEvent?, Any, ManifoldRunnerContext>(false, true, String::class.java) {
    val node: ManifoldEventNode
    val msg: ManifoldEvent

    var received = CompletableFuture<ManifoldEvent>()

    init {
        node = ManifoldEventBus.register("test.msg.action.B2", arrayOf("group2"))
        msg = ManifoldEvent(TestEvent.TestB, "2")

        val replyMsg = ManifoldEvent(TestEvent.TestB, "3")

        node.on(TestEvent.TestA) { e ->
            received.complete(e)

            node.reply(e, replyMsg)
        }
    }

    override fun core() = null

    fun send() {
        node.send("test.msg.action.A", msg)
    }
}