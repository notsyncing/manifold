package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import io.github.notsyncing.manifold.tests.toys.TestAction
import io.github.notsyncing.manifold.tests.toys.TestManager
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

class ManifoldTest {
    @Before
    fun setUp() {
        Manifold.reset()

        Manifold.dependencyProvider = Mockito.mock(ManifoldDependencyProvider::class.java)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestManager::class.java)).thenReturn(null)
        Mockito.`when`(Manifold.dependencyProvider?.get(TestAction::class.java)).thenReturn(TestAction(null, null))
    }

    @Test
    fun testExecute() {
        val r = Manifold.from<TestAction>().hello().get()
        Assert.assertEquals("Hello", r)
    }

    @Test
    fun testRegisterAction() {
        val a = TestAction(null, null)
        Manifold.registerAction(a)

        Assert.assertEquals(a, Manifold.getAction(a.javaClass))
    }
}