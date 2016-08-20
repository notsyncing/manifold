package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.event.EventSendType
import io.github.notsyncing.manifold.eventbus.event.EventType
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.eventbus.exceptions.NodeNotFoundException
import io.github.notsyncing.manifold.tests.toys.*
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

class ManifoldEventBusTest {
    @Before
    fun setUp() {
        ManifoldEventBus.debug = true
        Manifold.init()

        Manifold.registerAction(TestMessageActionA::class.java)
        Manifold.registerAction(TestMessageActionB1::class.java)
        Manifold.registerAction(TestMessageActionB2::class.java)
        Manifold.registerAction(TestMessageActionB3::class.java)
    }

    @After
    fun tearDown() {
        Manifold.destroy().get()
    }

    @Test
    fun testSendSimpleLocalMessage() {
        async<Unit> {
            Manifold.run(TestMessageActionA::class.java) { it.send() }

            val e = await(Manifold.getAction(TestMessageActionB1::class.java).received)

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
        async<Unit> {
            val r = await(Manifold.run(TestMessageActionA::class.java) { it.sendAndWaitForReply() })
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
        var f1 = Manifold.getAction(TestMessageActionB1::class.java).received
        var f2 = Manifold.getAction(TestMessageActionB2::class.java).received
        var f3 = Manifold.getAction(TestMessageActionB3::class.java).received

        async<Unit> {
            Manifold.run(TestMessageActionA::class.java) { it.sendToAnyInGroup() }
            var e1 = await(Manifold.getAction(TestMessageActionB1::class.java).received)
            Assert.assertEquals("1", e1.data)
            Assert.assertFalse(f2.isDone)
            Assert.assertFalse(f3.isDone)
            f1 = CompletableFuture<ManifoldEvent<TestEvent>>()
            Manifold.getAction(TestMessageActionB1::class.java).received = f1

            Manifold.run(TestMessageActionA::class.java) { it.sendToAnyInGroup() }
            val e2 = await(Manifold.getAction(TestMessageActionB2::class.java).received)
            Assert.assertEquals("1", e2.data)
            Assert.assertFalse(f1.isDone)
            Assert.assertFalse(f3.isDone)
            f2 = CompletableFuture<ManifoldEvent<TestEvent>>()
            Manifold.getAction(TestMessageActionB2::class.java).received = f2

            Manifold.run(TestMessageActionA::class.java) { it.sendToAnyInGroup() }
            val e3 = await(Manifold.getAction(TestMessageActionB3::class.java).received)
            Assert.assertEquals("1", e3.data)
            Assert.assertFalse(f1.isDone)
            Assert.assertFalse(f2.isDone)
            f3 = CompletableFuture<ManifoldEvent<TestEvent>>()
            Manifold.getAction(TestMessageActionB3::class.java).received = f3

            Manifold.run(TestMessageActionA::class.java) { it.sendToAnyInGroup() }
            e1 = await(Manifold.getAction(TestMessageActionB1::class.java).received)
            Assert.assertEquals("1", e1.data)
            Assert.assertFalse(f2.isDone)
            Assert.assertFalse(f3.isDone)
            f1 = CompletableFuture<ManifoldEvent<TestEvent>>()
            Manifold.getAction(TestMessageActionB1::class.java).received = f1
        }.get()
    }

    @Test
    fun testSendMessageToNonExistsNode() {
        val f = Manifold.run(TestMessageActionA::class.java) { it.sendToNonExists() }

        try {
            f.get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is NodeNotFoundException)
        }
    }

    @Test
    fun testSendMessageAndReplyTimeout() {
        val f = Manifold.run(TestMessageActionA::class.java) { it.sendAndWaitForReplyTimeout() }

        try {
            f.get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is TimeoutException)
        }
    }
}