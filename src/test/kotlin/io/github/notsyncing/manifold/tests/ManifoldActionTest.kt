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
    @Before
    fun setUp() {
        Manifold.reset()
    }

    @Test
    fun testExecute() {
        val action = TestAction(null, null)
        val s = action.hello().get()

        Assert.assertEquals("Hello", s)
    }
}
