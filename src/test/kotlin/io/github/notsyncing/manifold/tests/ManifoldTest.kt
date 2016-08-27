package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import io.github.notsyncing.manifold.ManifoldTransactionProvider
import io.github.notsyncing.manifold.action.ManifoldTransaction
import io.github.notsyncing.manifold.tests.toys.TestAction
import io.github.notsyncing.manifold.tests.toys.TestManager
import kotlinx.coroutines.async
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

        Manifold.dependencyProvider = Mockito.mock(ManifoldDependencyProvider::class.java)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestManager::class.java)).thenReturn(null)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestAction::class.java)).thenReturn(TestAction(null, null))

        Manifold.transactionProvider = object : ManifoldTransactionProvider {
            override fun get(): ManifoldTransaction<*> {
                return object : ManifoldTransaction<String>("") {
                    override fun begin(withTransaction: Boolean): CompletableFuture<Void> {
                        if (withTransaction) {
                            beganWithTrans = true
                        } else {
                            beganWithoutTrans = true
                        }

                        return CompletableFuture.completedFuture(null)
                    }

                    override fun commit(): CompletableFuture<Void> {
                        committed = true
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun end(): CompletableFuture<Void> {
                        ended = true
                        return CompletableFuture.completedFuture(null)
                    }
                }
            }
        }
    }

    @Test
    fun testExecute() {
        val r = Manifold.run(TestAction::class.java) { it.hello() }.get()
        Assert.assertEquals("Hello", r)

        Assert.assertFalse(beganWithTrans)
        Assert.assertTrue(beganWithoutTrans)
        Assert.assertFalse(committed)
        //Assert.assertTrue(ended)
    }

    @Test
    fun testRegisterAction() {
        val a = TestAction(null, null)
        Manifold.registerAction(a)

        Assert.assertEquals(a, Manifold.getAction(TestAction::class.java))
    }

    @Test
    fun testRunActionWithInvokeClosure() {
        Manifold.run { m ->
            async<String> {
                return@async await(m(TestAction::class.java) { it.hello() })
            }
        }.get()
    }

    @Test
    fun testRunActionWithInvoke() {
        Manifold.run { m ->
            async<String> {
                return@async await(m(TestAction::class.java)())
            }
        }.get()
    }
}