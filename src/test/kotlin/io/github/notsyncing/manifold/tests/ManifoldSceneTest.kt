package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import io.github.notsyncing.manifold.ManifoldTransactionProvider
import io.github.notsyncing.manifold.action.ManifoldTransaction
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
        TestSceneWithBackground.reset()
        TestSceneFailedException.reset()
        TestSceneTransaction.reset()
        TestSceneWithSuccessCond.reset()

        Manifold.enableFeatureManagement = false
        Manifold.reset()

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
        Manifold.dependencyProvider = Mockito.mock(ManifoldDependencyProvider::class.java)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestManager::class.java)).thenReturn(testManager)

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

    private fun createTransProviderForBackground() {
        Manifold.transactionProvider = object : ManifoldTransactionProvider {
            override fun get(startStack: Exception): ManifoldTransaction<*> {
                return object : ManifoldTransaction<String>("") {
                    override fun begin(withTransaction: Boolean): CompletableFuture<Void> {
                        TestSceneWithBackground.add(TestSceneWithBackground.TRANS_START)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun commit(endTransaction: Boolean): CompletableFuture<Void> {
                        TestSceneWithBackground.add(TestSceneWithBackground.TRANS_COMMIT)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun end(): CompletableFuture<Void> {
                        TestSceneWithBackground.add(TestSceneWithBackground.TRANS_END)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun rollback(endTransaction: Boolean): CompletableFuture<Void> {
                        TestSceneWithBackground.add(TestSceneWithBackground.TRANS_ROLLBACK)
                        return CompletableFuture.completedFuture(null)
                    }
                }
            }
        }
    }

    @Test
    fun testRunInBackgroundWithTrans() {
        createTransProviderForBackground()
        Manifold.run(TestSceneWithBackground(keepTrans = true), "test_bg_session")

        Thread.sleep(1000)

        val expected = arrayOf(TestSceneWithBackground.SCENE_START, TestSceneWithBackground.TRANS_START,
                TestSceneWithBackground.SCENE_END, TestSceneWithBackground.RUN_BG_END,
                TestSceneWithBackground.TRANS_COMMIT, TestSceneWithBackground.TRANS_END)
        Assert.assertArrayEquals(expected, TestSceneWithBackground.list.toTypedArray())
    }

    @Test
    fun testRunInBackgroundWithTransAndException() {
        createTransProviderForBackground()
        Manifold.run(TestSceneWithBackground(keepTrans = true, throwException = true), "test_bg_session_exp")

        Thread.sleep(1000)

        Assert.assertEquals("test", TestSceneWithBackground.ex?.message)

        val expected = arrayOf(TestSceneWithBackground.SCENE_START, TestSceneWithBackground.TRANS_START,
                TestSceneWithBackground.SCENE_END, TestSceneWithBackground.TRANS_ROLLBACK,
                TestSceneWithBackground.RUN_BG_EXCEPTION, TestSceneWithBackground.TRANS_COMMIT,
                TestSceneWithBackground.TRANS_END)
        Assert.assertArrayEquals(expected, TestSceneWithBackground.list.toTypedArray())
    }

    @Test
    fun testRunInBackgroundWithoutTrans() {
        createTransProviderForBackground()
        Manifold.run(TestSceneWithBackground(keepTrans = false), "test_bg_session")

        Thread.sleep(1000)

        val expected = arrayOf(TestSceneWithBackground.SCENE_START, TestSceneWithBackground.TRANS_START,
                TestSceneWithBackground.SCENE_END, TestSceneWithBackground.TRANS_COMMIT,
                TestSceneWithBackground.TRANS_END, TestSceneWithBackground.RUN_BG_END)
        Assert.assertArrayEquals(expected, TestSceneWithBackground.list.toTypedArray())
    }

    @Test
    fun testExceptionInStage() {
        try {
            Manifold.run(TestSceneSimpleException()).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is RuntimeException)
            Assert.assertEquals("Here!", e.cause?.message)
        }
    }

    private fun createTransProviderForFailedException() {
        Manifold.transactionProvider = object : ManifoldTransactionProvider {
            override fun get(startStack: Exception): ManifoldTransaction<*> {
                return object : ManifoldTransaction<String>("") {
                    override fun begin(withTransaction: Boolean): CompletableFuture<Void> {
                        TestSceneFailedException.add(TestSceneWithBackground.TRANS_START)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun commit(endTransaction: Boolean): CompletableFuture<Void> {
                        TestSceneFailedException.add(TestSceneWithBackground.TRANS_COMMIT)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun end(): CompletableFuture<Void> {
                        TestSceneFailedException.add(TestSceneWithBackground.TRANS_END)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun rollback(endTransaction: Boolean): CompletableFuture<Void> {
                        return CompletableFuture.completedFuture(null)
                    }
                }
            }
        }
    }

    @Test
    fun testSceneFailedExceptionInStage() {
        createTransProviderForFailedException()

        val r = Manifold.run(TestSceneFailedException()).get()
        Assert.assertEquals("Failed!", r)

        val expected = arrayOf(TestSceneFailedException.SCENE_START, TestSceneFailedException.TRANS_START,
                TestSceneFailedException.TRANS_END)
        Assert.assertArrayEquals(expected, TestSceneFailedException.list.toTypedArray())
    }

    private fun createTransProvider() {
        Manifold.transactionProvider = object : ManifoldTransactionProvider {
            override fun get(startStack: Exception): ManifoldTransaction<*> {
                return object : ManifoldTransaction<String>("") {
                    override fun begin(withTransaction: Boolean): CompletableFuture<Void> {
                        TestSceneTransaction.add(TestSceneWithBackground.TRANS_START)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun commit(endTransaction: Boolean): CompletableFuture<Void> {
                        TestSceneTransaction.add(TestSceneWithBackground.TRANS_COMMIT)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun end(): CompletableFuture<Void> {
                        TestSceneTransaction.add(TestSceneWithBackground.TRANS_END)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun rollback(endTransaction: Boolean): CompletableFuture<Void> {
                        return CompletableFuture.completedFuture(null)
                    }
                }
            }
        }
    }

    @Test
    fun testSceneWithTransaction() {
        createTransProvider()

        val r = Manifold.run(TestSceneTransaction()).get()
        Assert.assertEquals("Hello!", r)

        val expected = arrayOf(TestSceneTransaction.SCENE_START, TestSceneTransaction.TRANS_START,
                TestSceneTransaction.SCENE_END, TestSceneTransaction.TRANS_COMMIT,
                TestSceneTransaction.TRANS_END)
        Assert.assertArrayEquals(expected, TestSceneTransaction.list.toTypedArray())
    }

    private fun createTransProviderForSuccessCond() {
        Manifold.transactionProvider = object : ManifoldTransactionProvider {
            override fun get(startStack: Exception): ManifoldTransaction<*> {
                return object : ManifoldTransaction<String>("") {
                    override fun begin(withTransaction: Boolean): CompletableFuture<Void> {
                        TestSceneWithSuccessCond.add(TestSceneWithSuccessCond.TRANS_START)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun commit(endTransaction: Boolean): CompletableFuture<Void> {
                        TestSceneWithSuccessCond.add(TestSceneWithSuccessCond.TRANS_COMMIT)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun end(): CompletableFuture<Void> {
                        TestSceneWithSuccessCond.add(TestSceneWithSuccessCond.TRANS_END)
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun rollback(endTransaction: Boolean): CompletableFuture<Void> {
                        TestSceneWithSuccessCond.add(TestSceneWithSuccessCond.TRANS_ROLLBACK)
                        return CompletableFuture.completedFuture(null)
                    }
                }
            }
        }
    }

    @Test
    fun testSceneWithSuccessConditionPass() {
        createTransProviderForSuccessCond()

        val r = Manifold.run(TestSceneWithSuccessCond(true)).get()
        Assert.assertEquals("Hello!", r)

        val expected = arrayOf(TestSceneWithSuccessCond.SCENE_START, TestSceneWithSuccessCond.TRANS_START,
                TestSceneWithSuccessCond.SCENE_END, TestSceneWithSuccessCond.TRANS_COMMIT,
                TestSceneWithSuccessCond.TRANS_END)
        Assert.assertArrayEquals(expected, TestSceneWithSuccessCond.list.toTypedArray())
    }

    @Test
    fun testSceneWithSuccessConditionDeny() {
        createTransProviderForSuccessCond()

        val r = Manifold.run(TestSceneWithSuccessCond(false)).get()
        Assert.assertEquals("Hello!", r)

        val expected = arrayOf(TestSceneWithSuccessCond.SCENE_START, TestSceneWithSuccessCond.TRANS_START,
                TestSceneWithSuccessCond.SCENE_END, TestSceneWithSuccessCond.TRANS_END)
        Assert.assertArrayEquals(expected, TestSceneWithSuccessCond.list.toTypedArray())
    }
}