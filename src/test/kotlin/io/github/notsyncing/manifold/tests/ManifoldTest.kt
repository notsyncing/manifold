package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.tests.toys.TestAction
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ManifoldTest {
    @Before
    fun setUp() {
        Manifold.reset()
    }

    @Test
    fun testExecute() {
        val r = Manifold.from<TestAction>().hello().get()
        Assert.assertEquals("Hello", r)
    }

    @Test
    fun testRegisterAction() {
        val a = TestAction()
        Manifold.registerAction(a)

        Assert.assertEquals(a, Manifold.getAction(a.javaClass))
    }
}