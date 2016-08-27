package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestMessageActionB1 : ManifoldAction<String, ManifoldEvent<TestEvent>?, Any>(false, true, String::class.java) {
    val node: ManifoldEventNode
    val msg: ManifoldEvent<TestEvent>

    var received = CompletableFuture<ManifoldEvent<TestEvent>>()

    init {
        node = ManifoldEventBus.register("test.msg.action.B1", arrayOf("group2"))
        msg = ManifoldEvent(TestEvent.TestB, "2")

        val replyMsg = ManifoldEvent<TestEvent>(TestEvent.TestB, "3")

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