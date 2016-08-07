package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.ManifoldManagerProvider
import io.github.notsyncing.manifold.tests.toys.TestAction
import io.github.notsyncing.manifold.tests.toys.TestManager
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ManifoldActionTest {
    @Before
    fun setUp() {
        Manifold.reset()
    }

    @Test
    fun testAutoProvide() {
        val provider = Mockito.mock(ManifoldManagerProvider::class.java)
        Mockito.`when`(provider.get(TestManager::class.java)).thenReturn(TestManager())

        Manifold.managerProvider = provider

        val action = TestAction()

        Assert.assertNotNull(TestAction.testManager)
        Assert.assertNull(TestAction.testManager2)
    }

    @Test
    fun testExecute() {
        val action = TestAction()
        val s = action.hello().get()

        Assert.assertEquals("Hello", s)
    }
}
