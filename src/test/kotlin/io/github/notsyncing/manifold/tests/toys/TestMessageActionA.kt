package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.ManifoldAction
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestMessageActionA : ManifoldAction<String, ManifoldEvent<TestEvent>?>(false, true, String::class.java) {
    val node: ManifoldEventNode
    val msg: ManifoldEvent<TestEvent>

    init {
        node = ManifoldEventBus.register("test.msg.action.A", arrayOf("group1"))
        msg = ManifoldEvent(TestEvent.TestA, "1")
    }

    fun send(): CompletableFuture<ManifoldEvent<TestEvent>?> {
        node.send("test.msg.action.B1", msg)

        return CompletableFuture.completedFuture(null)
    }

    fun sendToNonExists(): CompletableFuture<ManifoldEvent<TestEvent>?> {
        node.send("test.msg.action.NOTEXISTS", msg)

        return CompletableFuture.completedFuture(null)
    }

    fun sendAndWaitForReply(): CompletableFuture<ManifoldEvent<TestEvent>?> {
        return node.sendAndWaitForReply("test.msg.action.B1", msg)
    }

    fun sendAndWaitForReplyTimeout(): CompletableFuture<ManifoldEvent<TestEvent>?> {
        msg.event = TestEvent.TestB
        return node.sendAndWaitForReply("test.msg.action.B1", msg, 2000)
    }

    fun sendToAnyInGroup(): CompletableFuture<ManifoldEvent<TestEvent>?> {
        node.sendToAnyInGroup("group2", msg)

        return CompletableFuture.completedFuture(null)
    }

    fun broadcast() {
        node.broadcast(msg)
    }

    fun broadcastToGroup() {
        node.broadcastToGroup("group1", msg)
    }
}