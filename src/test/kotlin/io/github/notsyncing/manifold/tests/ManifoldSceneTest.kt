package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.tests.toys.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ManifoldSceneTest {
    private val testManager = Mockito.mock(TestManager::class.java)

    @Before
    fun setUp() {
        TestScene.initExecuted = false
        TestScene.recvEvent = CompletableFuture()

        TestSceneSecond.reset()

        Manifold.reset()

        Manifold.dependencyProvider = Mockito.mock(ManifoldDependencyProvider::class.java)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestManager::class.java)).thenReturn(testManager)

        ManifoldEventBus.debug = true
        Manifold.init()
    }

    @After
    fun tearDown() {
        Manifold.destroy().get()
    }

    @Test
    fun testInit() {
        Assert.assertTrue(TestScene.initExecuted)
    }

    @Test
    fun testAutoProvide() {
        val scene = TestScene()

        Assert.assertEquals(testManager, scene.testManager)
        Assert.assertEquals(testManager, scene.testManager2)
    }

    @Test
    fun testAwakeOnEvent() {
        val node = ManifoldEventBus.register("test.tester")
        val eventSent = ManifoldEvent(TestEvent.TestA, "test")
        node.send("test.scene", eventSent)

        TestScene.recvEvent.thenAccept {
            Assert.assertNotNull(it)
            Assert.assertEquals(eventSent, it)
        }.get(5, TimeUnit.SECONDS)
    }

    @Test
    fun testTransitionToWithEvent() {
        Manifold.run(TestSceneFirst())

        TestSceneSecond.recvEvent.thenAccept {
            Assert.assertNotNull(it)
            Assert.assertEquals("Test data", it.data)
        }.get(5, TimeUnit.SECONDS)
    }

    @Test
    fun testTransitionToWithConstruct() {
        Manifold.run(TestSceneFirst2())

        TestSceneSecond.contEvent.thenAccept {
            Assert.assertEquals(2, TestSceneSecond.id)
            Assert.assertEquals("Test data", TestSceneSecond.name)
        }.get(5, TimeUnit.SECONDS)
    }

    @Test
    fun testTransitionToWithSessionIdentifier() {
        Manifold.run(TestSceneFirst(), "test_session")

        TestSceneSecond.recvEvent.thenAccept {
            Assert.assertEquals("test_session", TestSceneSecond.sessionId)
        }.get(5, TimeUnit.SECONDS)
    }
}