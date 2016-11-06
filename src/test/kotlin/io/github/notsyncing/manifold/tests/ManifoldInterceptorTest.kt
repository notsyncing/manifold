package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.InterceptorException
import io.github.notsyncing.manifold.action.interceptors.InterceptorResult
import io.github.notsyncing.manifold.tests.toys.TestActionSimple
import io.github.notsyncing.manifold.tests.toys.TestActionSimple2
import io.github.notsyncing.manifold.tests.toys.TestSceneSimple
import io.github.notsyncing.manifold.tests.toys.TestSceneSimple2
import io.github.notsyncing.manifold.tests.toys.interceptor.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ManifoldInterceptorTest {
    @Before
    fun setUp() {
        TestActionInterceptor.reset()
        TestSceneInterceptor.reset()

        Manifold.init()
    }

    @After
    fun tearDown() {
        Manifold.destroy().get()
    }

    @Test
    fun testActionInterceptor() {
        Manifold.run(TestActionSimple()).get()

        Assert.assertNotNull(TestActionInterceptor.context)
        Assert.assertTrue(TestActionInterceptor.beforeCalled)
        Assert.assertTrue(TestActionInterceptor.afterCalled)
        Assert.assertNull(TestActionInterceptor.context?.annotation)
        Assert.assertEquals("Hello", TestActionInterceptor.context?.result)
        Assert.assertEquals(InterceptorResult.Continue, TestActionInterceptor.context?.interceptorResult)
    }

    @Test
    fun testActionInterceptorStop() {
        TestActionInterceptor.makeStop = true

        try {
            Manifold.run(TestActionSimple()).get()
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is InterceptorException)
        }

        Assert.assertNotNull(TestActionInterceptor.context)
        Assert.assertTrue(TestActionInterceptor.beforeCalled)
        Assert.assertFalse(TestActionInterceptor.afterCalled)
        Assert.assertNull(TestActionInterceptor.context?.result)
        Assert.assertEquals(InterceptorResult.Stop, TestActionInterceptor.context?.interceptorResult)
    }

    @Test
    fun testActionInterceptorChangeResult() {
        TestActionInterceptor.changeResult = true
        Manifold.run(TestActionSimple()).get()

        Assert.assertNotNull(TestActionInterceptor.context)
        Assert.assertTrue(TestActionInterceptor.beforeCalled)
        Assert.assertTrue(TestActionInterceptor.afterCalled)
        Assert.assertEquals("Changed", TestActionInterceptor.context?.result)
        Assert.assertEquals(InterceptorResult.Continue, TestActionInterceptor.context?.interceptorResult)
    }

    @Test
    fun testSceneInterceptor() {
        Manifold.run(TestSceneSimple()).get()

        Assert.assertNotNull(TestSceneInterceptor.context)
        Assert.assertTrue(TestSceneInterceptor.beforeCalled)
        Assert.assertTrue(TestSceneInterceptor.afterCalled)
        Assert.assertNull(TestSceneInterceptor.context?.annotation)
        Assert.assertEquals("Hello!", TestSceneInterceptor.context?.result)
        Assert.assertEquals(InterceptorResult.Continue, TestSceneInterceptor.context?.interceptorResult)
    }

    @Test
    fun testSceneInterceptorStop() {
        TestSceneInterceptor.makeStop = true

        try {
            Manifold.run(TestSceneSimple()).get()
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is InterceptorException)
        }

        Assert.assertNotNull(TestSceneInterceptor.context)
        Assert.assertTrue(TestSceneInterceptor.beforeCalled)
        Assert.assertFalse(TestSceneInterceptor.afterCalled)
        Assert.assertNull(TestSceneInterceptor.context?.result)
        Assert.assertEquals(InterceptorResult.Stop, TestSceneInterceptor.context?.interceptorResult)
    }

    @Test
    fun testSceneInterceptorChangeResult() {
        TestSceneInterceptor.changeResult = true
        Manifold.run(TestSceneSimple()).get()

        Assert.assertNotNull(TestSceneInterceptor.context)
        Assert.assertTrue(TestSceneInterceptor.beforeCalled)
        Assert.assertTrue(TestSceneInterceptor.afterCalled)
        Assert.assertEquals("Changed", TestSceneInterceptor.context?.result)
        Assert.assertEquals(InterceptorResult.Continue, TestSceneInterceptor.context?.interceptorResult)
    }

    @Test
    fun testSceneInterceptorForAnnotation() {
        Manifold.run(TestSceneSimple2()).get()

        Assert.assertNotNull(TestSceneInterceptor.context)
        Assert.assertEquals(TestSceneInterceptorAnno::class, TestSceneInterceptor.context?.annotation?.annotationClass)
    }

    @Test
    fun testActionInterceptorForAnnotation() {
        Manifold.run(TestActionSimple2()).get()

        Assert.assertNotNull(TestActionInterceptor.context)
        Assert.assertEquals(TestActionInterceptorAnno::class, TestActionInterceptor.context?.annotation?.annotationClass)
    }

    @Test
    fun testSceneInterceptorForEveryScene() {
        Manifold.run(TestSceneSimple()).get()

        Assert.assertNotNull(TestGlobalSceneInterceptor.context)
        Assert.assertTrue(TestGlobalSceneInterceptor.beforeCalled)
        Assert.assertTrue(TestGlobalSceneInterceptor.afterCalled)
        Assert.assertNull(TestGlobalSceneInterceptor.context?.annotation)
        Assert.assertEquals("Hello!", TestGlobalSceneInterceptor.context?.result)
        Assert.assertEquals(InterceptorResult.Continue, TestGlobalSceneInterceptor.context?.interceptorResult)
    }
}