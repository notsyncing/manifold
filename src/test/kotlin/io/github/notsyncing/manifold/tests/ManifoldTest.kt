package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import io.github.notsyncing.manifold.ManifoldTransactionProvider
import io.github.notsyncing.manifold.action.ManifoldTransaction
import io.github.notsyncing.manifold.action.SceneContext
import io.github.notsyncing.manifold.tests.toys.TestAction
import io.github.notsyncing.manifold.tests.toys.TestManager
import io.github.notsyncing.manifold.tests.toys.TestSceneTransaction
import io.github.notsyncing.manifold.tests.toys.TestStorage
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.CompletableFuture

class ManifoldTest {
    private var beganWithTrans = false
    private var beganWithoutTrans = false
    private var committed = false
    private var ended = false

    @Before
    fun setUp() {
        beganWithTrans = false
        beganWithoutTrans = false
        committed = false
        ended = false

        Manifold.reset()

        Manifold.enableFeatureManagement = false

        Manifold.dependencyProvider = Mockito.mock(ManifoldDependencyProvider::class.java)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestManager::class.java)).thenReturn(null)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestStorage::class.java)).thenReturn(TestStorage())

        Manifold.interceptors.reset()

        Manifold.transactionProvider = object : ManifoldTransactionProvider {
            override fun get(startStack: Exception): ManifoldTransaction<*> {
                return object : ManifoldTransaction<String>("") {
                    override fun begin(withTransaction: Boolean): CompletableFuture<Void> {
                        if (withTransaction) {
                            beganWithTrans = true
                        } else {
                            beganWithoutTrans = true
                        }

                        return CompletableFuture.completedFuture(null)
                    }

                    override fun commit(endTransaction: Boolean): CompletableFuture<Void> {
                        committed = true
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun end(): CompletableFuture<Void> {
                        ended = true
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
    fun testExecuteAction() {
        val a = TestAction()
        a.context = SceneContext()

        val r = Manifold.run(a).get()
        Assert.assertEquals("Hello", r)

        Assert.assertFalse(beganWithTrans)
        Assert.assertTrue(beganWithoutTrans)
        Assert.assertFalse(committed)
        Assert.assertTrue(ended)
    }

    @Test
    fun testExecuteSceneWithTransaction() {
        val s = TestSceneTransaction()

        val r = Manifold.run(s).get()
        Assert.assertEquals("Hello!", r)

        Assert.assertTrue(beganWithTrans)
        Assert.assertFalse(beganWithoutTrans)
        Assert.assertTrue(committed)
        Assert.assertTrue(ended)
    }

    @Test
    fun testExecuteSceneWithoutTransaction() {
        val s = TestSceneTransaction(useTrans = false)

        val r = Manifold.run(s).get()
        Assert.assertEquals("Hello!", r)

        Assert.assertFalse(beganWithTrans)
        Assert.assertTrue(beganWithoutTrans)
        Assert.assertFalse(committed)
        Assert.assertTrue(ended)
    }

    @Test
    fun testRunActionWithInvoke() {
        Manifold.run { m ->
            future {
                val s = m(TestAction()).await()

                Assert.assertEquals("Hello", s)
            }
        }
    }
}