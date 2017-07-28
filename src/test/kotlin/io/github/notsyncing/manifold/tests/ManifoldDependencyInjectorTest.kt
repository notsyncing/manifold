package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.di.ManifoldDependencyInjector
import io.github.notsyncing.manifold.domain.ManifoldDomain
import io.github.notsyncing.manifold.tests.toys.TestAnno
import io.github.notsyncing.manifold.tests.toys.di.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ManifoldDependencyInjectorTest {
    var di = ManifoldDependencyInjector(ManifoldDomain().apply { this.init() })

    @Before
    fun setUp() {
        di = ManifoldDependencyInjector(ManifoldDomain().apply { this.init() })
        di.init()

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
        Assert.assertNotNull(c?.a)
        Assert.assertNotNull(c?.b)
    }

    @Test
    fun testDeepInjection() {
        val d = di.get(D::class.java)

        Assert.assertNotNull(d)
        Assert.assertNotNull(d?.b)
        Assert.assertNotNull(d?.c)
        Assert.assertNotNull(d?.c?.a)
        Assert.assertNotNull(d?.c?.b)
    }

    @Test
    fun testNoProvide() {
        val e = di.get(E::class.java)

        Assert.assertNotNull(e)
        Assert.assertNotNull(e?.a)
        Assert.assertNotNull(e?.b)
        Assert.assertNull(e?.c)
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

        Assert.assertEquals(a, c?.a)
    }

    @Test
    fun testRegisterAs() {
        val ac = AC()
        di.registerAs(ac, A::class.java)
        val c = di.get(C::class.java)

        Assert.assertEquals(ac, c?.a)
    }

    @Test
    fun testRegisterMapping() {
        di.registerMapping(A::class.java, IA::class.java)
        val a = di.get(IA::class.java)

        Assert.assertEquals(A::class.java, a?.let { it::class.java })
    }

    @Test
    fun testRegisterSingleton() {
        di.registerSingleton(B::class.java)
        val b1 = di.get(B::class.java)
        val b2 = di.get(B::class.java)

        Assert.assertEquals(b1, b2)
    }

    @Test
    fun testGetAllAnnotated() {
        val l = di.getAllAnnotated(TestAnno::class.java)

        Assert.assertEquals(3, l.size)
        Assert.assertTrue(l.contains(A::class.java))
        Assert.assertTrue(l.contains(B::class.java))
        Assert.assertTrue(l.contains(C::class.java))
    }

    @Test
    fun testGetAllSubclasses() {
        val l = di.getAllSubclasses(A::class.java)

        Assert.assertEquals(3, l.size)
        Assert.assertTrue(l.contains<Any>(AC::class.java))
        Assert.assertTrue(l.contains<Any>(AC2::class.java))
        Assert.assertTrue(l.contains<Any>(AC3::class.java))
    }

    @Test
    fun testGetAllClassesImplemented() {
        val l = di.getAllClassesImplemented(IA::class.java)

        Assert.assertEquals(4, l.size)
        Assert.assertTrue(l.contains<Any>(A::class.java))
        Assert.assertTrue(l.contains<Any>(AC::class.java))
        Assert.assertTrue(l.contains<Any>(AC2::class.java))
        Assert.assertTrue(l.contains<Any>(AC3::class.java))
    }

    @Test
    fun testGetSystemClass() {
        val s = di.get(Any::class.java)

        Assert.assertNotNull(s)
        Assert.assertEquals(Any::class.java.name, s?.javaClass?.name)
    }
}