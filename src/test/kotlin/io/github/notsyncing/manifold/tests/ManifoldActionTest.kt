package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.ManifoldDependencyProvider
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
}
