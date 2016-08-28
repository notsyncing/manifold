package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ManifoldSceneTest {
    @Before
    fun setUp() {
        TestScene.initExecuted = false
        TestScene.recvEvent = CompletableFuture()

        Manifold.init()
    }

    @After
    fun tearDown() {
        Manifold.destroy()
    }

    @Test
    fun testInit() {
        Assert.assertTrue(TestScene.initExecuted)
    }

    @Test
    fun testAwakeOnEvent() {
        val node = ManifoldEventBus.register("test.tester")
        val eventSent = ManifoldEvent<TestEvent>(TestEvent.TestA, "test")
        node.send("test.scene", eventSent)

        TestScene.recvEvent.thenAccept {
            Assert.assertNotNull(it)
            Assert.assertEquals(eventSent, it)
        }.get(5, TimeUnit.SECONDS)
    }
}