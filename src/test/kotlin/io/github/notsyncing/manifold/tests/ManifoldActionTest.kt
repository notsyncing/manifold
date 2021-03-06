package io.github.notsyncing.manifold.tests

import com.nhaarman.mockito_kotlin.eq
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import io.github.notsyncing.manifold.ManifoldTransactionProvider
import io.github.notsyncing.manifold.action.ManifoldTransaction
import io.github.notsyncing.manifold.action.SceneContext
import io.github.notsyncing.manifold.action.session.ManifoldSessionStorageProvider
import io.github.notsyncing.manifold.tests.toys.TestAction
import io.github.notsyncing.manifold.tests.toys.TestManager
import io.github.notsyncing.manifold.tests.toys.TestStorage
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.CompletableFuture

class ManifoldActionTest {
    private val testManager = Mockito.mock(TestManager::class.java)

    @Before
    fun setUp() {
        Manifold.reset()
        TestStorage.reset()

        Manifold.dependencyProvider = Mockito.mock(ManifoldDependencyProvider::class.java)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestManager::class.java)).thenReturn(testManager)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestStorage::class.java)).thenReturn(TestStorage())

        Manifold.sessionStorageProvider = Mockito.mock(ManifoldSessionStorageProvider::class.java)
        Mockito.`when`(Manifold.sessionStorageProvider?.get<String>(eq("test_session"), eq("testVar")))
                .thenReturn("hello")

        Manifold.transactionProvider = object : ManifoldTransactionProvider {
            override fun get(startStack: Exception): ManifoldTransaction<*> {
                return object : ManifoldTransaction<String>("storage") {
                    override fun begin(withTransaction: Boolean): CompletableFuture<Void> {
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun commit(endTransaction: Boolean): CompletableFuture<Void> {
                        return CompletableFuture.completedFuture(null)
                    }

                    override fun end(): CompletableFuture<Void> {
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
    fun testExecute() {
        val action = TestAction()
        action.context = SceneContext()
        val s = action.execute().get()

        Assert.assertEquals("Hello", s)
    }

    @Test
    fun testAutoProvide() {
        val action = TestAction()
        action.context = SceneContext()

        Assert.assertEquals(testManager, action.testManager)
        Assert.assertEquals(testManager, action.testManager2)
    }

    @Test
    fun testStorage() {
        val action = TestAction()
        action.context = SceneContext()

        action.execute().get()

        Assert.assertEquals("storage", TestStorage.result)
    }

    @Test
    fun testSessionVariable() {
        val action = TestAction()
        action.sessionIdentifier = "test_session"
        Assert.assertEquals("hello", action.testVar)
    }
}
