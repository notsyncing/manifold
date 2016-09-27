package io.github.notsyncing.manifold.tests

import com.nhaarman.mockito_kotlin.eq
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import io.github.notsyncing.manifold.action.session.ManifoldSessionStorageProvider
import io.github.notsyncing.manifold.tests.toys.TestAction
import io.github.notsyncing.manifold.tests.toys.TestManager
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ManifoldActionTest {
    private val testManager = Mockito.mock(TestManager::class.java)

    @Before
    fun setUp() {
        Manifold.reset()

        Manifold.dependencyProvider = Mockito.mock(ManifoldDependencyProvider::class.java)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestManager::class.java)).thenReturn(testManager)

        Manifold.sessionStorageProvider = Mockito.mock(ManifoldSessionStorageProvider::class.java)
        Mockito.`when`(Manifold.sessionStorageProvider?.get<String>(eq("test_session"), eq("testVar")))
                .thenReturn("hello")
    }

    @Test
    fun testExecute() {
        val action = TestAction()
        val s = action.action().get()

        Assert.assertEquals("Hello", s)
    }

    @Test
    fun testAutoProvide() {
        val action = TestAction()
        Assert.assertEquals(testManager, action.testManager)
        Assert.assertEquals(testManager, action.testManager2)
    }

    @Test
    fun testSessionVariable() {
        val action = TestAction()
        action.sessionIdentifier = "test_session"
        Assert.assertEquals("hello", action.testVar)
    }
}
