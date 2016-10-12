package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.InterceptorResult
import io.github.notsyncing.manifold.tests.toys.TestActionInterceptor
import io.github.notsyncing.manifold.tests.toys.TestActionSimple
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ManifoldInterceptorTest {
    @Before
    fun setUp() {
        TestActionInterceptor.reset()
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
        Assert.assertEquals("Hello", TestActionInterceptor.context?.result)
        Assert.assertEquals(InterceptorResult.Continue, TestActionInterceptor.context?.interceptorResult)
    }
}