package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.EventSendType
import io.github.notsyncing.manifold.eventbus.event.EventType
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.eventbus.exceptions.NodeNotFoundException
import io.github.notsyncing.manifold.tests.toys.TestEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

class ManifoldEventBusTest {
    lateinit var nodeA: ManifoldEventNode
    lateinit var nodeB1: ManifoldEventNode
    lateinit var nodeB2: ManifoldEventNode
    lateinit var nodeB3: ManifoldEventNode
    lateinit var msgA: ManifoldEvent
    lateinit var receivedB1: CompletableFuture<ManifoldEvent>
    lateinit var receivedB2: CompletableFuture<ManifoldEvent>
    lateinit var receivedB3: CompletableFuture<ManifoldEvent>

    @Before
    fun setUp() {
        ManifoldEventBus.debug = true
        Manifold.init()

        receivedB1 = CompletableFuture<ManifoldEvent>()
        receivedB2 = CompletableFuture<ManifoldEvent>()
        receivedB3 = CompletableFuture<ManifoldEvent>()

        nodeA = ManifoldEventBus.register("test.msg.action.A", "group1")
        msgA = ManifoldEvent(TestEvent.TestA, "1")

        nodeB1 = ManifoldEventBus.register("test.msg.action.B1", "group2")
        nodeB2 = ManifoldEventBus.register("test.msg.action.B2", "group2")
        nodeB3 = ManifoldEventBus.register("test.msg.action.B3", "group2")

        val replyMsg = ManifoldEvent(TestEvent.TestB, "3")

        nodeB1.on(TestEvent.TestA) { e ->
            nodeB1.reply(e, replyMsg)

            receivedB1.complete(e)
        }

        nodeB2.on(TestEvent.TestA) { e ->
            nodeB2.reply(e, replyMsg)

            receivedB2.complete(e)
        }

        nodeB3.on(TestEvent.TestA) { e ->
            nodeB3.reply(e, replyMsg)

            receivedB3.complete(e)
        }
    }

    @After
    fun tearDown() {
        Manifold.destroy().get()
    }

    @Test
    fun testSendSimpleLocalMessage() {
        async {
            nodeA.send("test.msg.action.B1", msgA)

            val e = receivedB1.await()

            Assert.assertEquals("1", e.data)
            Assert.assertEquals("test.msg.action.A", e.source)
            Assert.assertEquals(TestEvent.TestA, e.event)
            Assert.assertEquals(EventSendType.Unicast, e.sendType)
            Assert.assertEquals(EventType.Data, e.type)
            Assert.assertEquals("test.msg.action.B1", e.target)
        }.get()
    }

    @Test
    fun testSendSimpleLocalMessageAndReply() {
        async {
            val r = nodeA.sendAndWaitForReply("test.msg.action.B1", msgA).await()
            Assert.assertNotNull(r)

            Assert.assertEquals("3", r?.data)
            Assert.assertEquals("test.msg.action.B1", r?.source)
            Assert.assertEquals(TestEvent.TestB, r?.event)
            Assert.assertEquals(EventSendType.Unicast, r?.sendType)
            Assert.assertEquals(EventType.Data, r?.type)
            Assert.assertEquals("test.msg.action.A", r?.target)
        }.get()
    }

    @Test
    fun testSendSimpleLocalMessageToAnyInGroup() {
        async {
            nodeA.sendToAnyInGroup("group2", msgA)
            var e1 = receivedB1.await()
            Assert.assertEquals("1", e1.data)
            Assert.assertFalse(receivedB2.isDone)
            Assert.assertFalse(receivedB3.isDone)
            receivedB1 = CompletableFuture<ManifoldEvent>()

            nodeA.sendToAnyInGroup("group2", msgA)
            val e2 = receivedB2.await()
            Assert.assertEquals("1", e2.data)
            Assert.assertFalse(receivedB1.isDone)
            Assert.assertFalse(receivedB3.isDone)
            receivedB2 = CompletableFuture<ManifoldEvent>()

            nodeA.sendToAnyInGroup("group2", msgA)
            val e3 = receivedB3.await()
            Assert.assertEquals("1", e3.data)
            Assert.assertFalse(receivedB1.isDone)
            Assert.assertFalse(receivedB2.isDone)
            receivedB3 = CompletableFuture<ManifoldEvent>()

            nodeA.sendToAnyInGroup("group2", msgA)
            e1 = receivedB1.await()
            Assert.assertEquals("1", e1.data)
            Assert.assertFalse(receivedB2.isDone)
            Assert.assertFalse(receivedB3.isDone)
            receivedB1 = CompletableFuture<ManifoldEvent>()
        }.get()
    }

    @Test
    fun testSendMessageToNonExistsNode() {
        try {
            nodeA.send("test.msg.action.NOTEXISTS", msgA)
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e is NodeNotFoundException)
        }
    }

    @Test
    fun testSendMessageAndReplyTimeout() {
        msgA.event = TestEvent.TestB
        val f = nodeA.sendAndWaitForReply("test.msg.action.B1", msgA, 2000)

        try {
            f.get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is TimeoutException)
        }
    }
}