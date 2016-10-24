package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.tests.toys.features.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ManifoldFeatureTest {
    @Before
    fun setUp() {
        Manifold.reset()
        Manifold.enableFeatureManagement = true

        TestFeatureScene1.reset()
        TestFeatureScene2.reset()
        TestFeatureScene3.reset()
        TestFeatureScene4.reset()
        TestFeatureScene5.reset()
        TestFeatureScene6.reset()
    }

    @After
    fun tearDown() {
        Manifold.destroy().get()
    }

    @Test
    fun testEnabledFeature() {
        Manifold.enableFeatures(TestFeatures.Feature1)
        Manifold.init()

        Assert.assertTrue(Manifold.isFeatureEnabled(TestFeatureScene1::class.java))
        Assert.assertTrue(TestFeatureScene1.initExecuted)
    }

    @Test
    fun testDisabledFeature() {
        Manifold.disableFeatures(TestFeatures.Feature1)
        Manifold.init()

        Assert.assertFalse(Manifold.isFeatureEnabled(TestFeatureScene1::class.java))
        Assert.assertFalse(TestFeatureScene1.initExecuted)
    }

    @Test
    fun testNotEnabledFeature() {
        Manifold.init()

        Assert.assertFalse(Manifold.isFeatureEnabled(TestFeatureScene1::class.java))
        Assert.assertFalse(TestFeatureScene1.initExecuted)
    }

    @Test
    fun testEnabledFeatureGroup() {
        Manifold.enableFeatureGroups(TestFeatures.FeatureGroup1)
        Manifold.init()

        Assert.assertFalse(Manifold.isFeatureEnabled(TestFeatureScene1::class.java))
        Assert.assertTrue(Manifold.isFeatureEnabled(TestFeatureScene2::class.java))
        Assert.assertTrue(Manifold.isFeatureEnabled(TestFeatureScene3::class.java))

        Assert.assertFalse(TestFeatureScene1.initExecuted)
        Assert.assertTrue(TestFeatureScene2.initExecuted)
        Assert.assertTrue(TestFeatureScene3.initExecuted)
    }

    @Test
    fun testDisabledFeatureGroup() {
        Manifold.disableFeatureGroups(TestFeatures.FeatureGroup1)
        Manifold.init()

        Assert.assertFalse(Manifold.isFeatureEnabled(TestFeatureScene1::class.java))
        Assert.assertFalse(Manifold.isFeatureEnabled(TestFeatureScene2::class.java))
        Assert.assertFalse(Manifold.isFeatureEnabled(TestFeatureScene3::class.java))

        Assert.assertFalse(TestFeatureScene1.initExecuted)
        Assert.assertFalse(TestFeatureScene2.initExecuted)
        Assert.assertFalse(TestFeatureScene3.initExecuted)
    }

    @Test
    fun testNotEnabledFeatureGroup() {
        Manifold.init()

        Assert.assertFalse(Manifold.isFeatureEnabled(TestFeatureScene2::class.java))
        Assert.assertFalse(Manifold.isFeatureEnabled(TestFeatureScene3::class.java))

        Assert.assertFalse(TestFeatureScene2.initExecuted)
        Assert.assertFalse(TestFeatureScene3.initExecuted)
    }

    @Test
    fun testDisabledSubFeatureGroup() {
        Manifold.enableFeatureGroups(TestFeatures.FeatureUpperGroup1)
        Manifold.disableFeatureGroups(TestFeatures.FeatureGroup3)
        Manifold.init()

        Assert.assertTrue(Manifold.isFeatureEnabled(TestFeatureScene4::class.java))
        Assert.assertTrue(Manifold.isFeatureEnabled(TestFeatureScene5::class.java))
        Assert.assertFalse(Manifold.isFeatureEnabled(TestFeatureScene6::class.java))

        Assert.assertTrue(TestFeatureScene4.initExecuted)
        Assert.assertTrue(TestFeatureScene5.initExecuted)
        Assert.assertFalse(TestFeatureScene6.initExecuted)
    }
}