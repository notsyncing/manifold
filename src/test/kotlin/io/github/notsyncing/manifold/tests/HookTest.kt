package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class HookTest {
    @Before
    fun setUp() {
        Manifold.init()
    }

    @After
    fun tearDown() {
        Manifold.destroy().get()
    }

    @Test
    fun testSimpleTypedHook() {
        val r = Manifold.run(TestHookScene()).get()
        Assert.assertEquals(4, r)
    }
}