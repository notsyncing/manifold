package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.di.ManifoldDependencyInjector
import io.github.notsyncing.manifold.tests.toys.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ManifoldDependencyInjectorTest {
    var di = ManifoldDependencyInjector()

    @Before
    fun setUp() {
        di = ManifoldDependencyInjector()

        E.counter = 0
    }

    @After
    fun tearDown() {
        di.reset()
    }

    @Test
    fun testSimpleInjection() {
        val c = di.get(C::class.java)

        Assert.assertNotNull(c)
        Assert.assertNotNull(c.a)
        Assert.assertNotNull(c.b)
    }

    @Test
    fun testDeepInjection() {
        val d = di.get(D::class.java)

        Assert.assertNotNull(d)
        Assert.assertNotNull(d.b)
        Assert.assertNotNull(d.c)
        Assert.assertNotNull(d.c?.a)
        Assert.assertNotNull(d.c?.b)
    }

    @Test
    fun testNoProvide() {
        val e = di.get(E::class.java)

        Assert.assertNotNull(e)
        Assert.assertNotNull(e.a)
        Assert.assertNotNull(e.b)
        Assert.assertNull(e.c)
    }

    @Test
    fun testProvideAsSingleton() {
        val e1 = di.get(E::class.java)
        val e2 = di.get(E::class.java)

        Assert.assertEquals(e1, e2)
        Assert.assertEquals(1, E.counter)
    }

    @Test
    fun testEarlyProvide() {
        Assert.assertTrue(di.has(F::class.java))
    }

    @Test
    fun testRegister() {
        val a = A()
        di.register(a)
        val c = di.get(C::class.java)

        Assert.assertEquals(a, c.a)
    }

    @Test
    fun testRegisterAs() {
        val ac = AC()
        di.registerAs(ac, A::class.java)
        val c = di.get(C::class.java)

        Assert.assertEquals(ac, c.a)
    }
}