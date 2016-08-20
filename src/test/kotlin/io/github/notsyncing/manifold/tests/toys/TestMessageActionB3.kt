package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.ManifoldAction
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestMessageActionB3 : ManifoldAction<String, ManifoldEvent<TestEvent>?>(false, true, String::class.java) {
    val node: ManifoldEventNode
    val msg: ManifoldEvent<TestEvent>

    var received = CompletableFuture<ManifoldEvent<TestEvent>>()

    init {
        node = ManifoldEventBus.register("test.msg.action.B3", arrayOf("group2"))
        msg = ManifoldEvent(TestEvent.TestB, "2")

        val replyMsg = ManifoldEvent<TestEvent>(TestEvent.TestB, "3")

        node.on(TestEvent.TestA) { e ->
            received.complete(e)

            node.reply(e, replyMsg)
        }
    }

    fun send() {
        node.send("test.msg.action.A", msg)
    }
}