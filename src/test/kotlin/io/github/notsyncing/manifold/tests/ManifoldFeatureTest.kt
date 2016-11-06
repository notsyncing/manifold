package io.github.notsyncing.manifold.tests

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.InterceptorException
import io.github.notsyncing.manifold.authenticate.AuthRole
import io.github.notsyncing.manifold.authenticate.AuthenticateInformationProvider
import io.github.notsyncing.manifold.authenticate.Permission
import io.github.notsyncing.manifold.authenticate.PermissionState
import io.github.notsyncing.manifold.feature.FeatureAuthenticator
import io.github.notsyncing.manifold.tests.toys.features.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture

class ManifoldFeatureTest {
    private val features = Manifold.features
    
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
        features.enableFeatures(TestFeatures.Feature1)
        Manifold.init()

        Assert.assertTrue(features.isFeatureEnabled(TestFeatureScene1::class.java))
        Assert.assertTrue(TestFeatureScene1.initExecuted)
    }

    @Test
    fun testDisabledFeature() {
        features.disableFeatures(TestFeatures.Feature1)
        Manifold.init()

        Assert.assertFalse(features.isFeatureEnabled(TestFeatureScene1::class.java))
        Assert.assertFalse(TestFeatureScene1.initExecuted)
    }

    @Test
    fun testNotEnabledFeature() {
        Manifold.init()

        Assert.assertFalse(features.isFeatureEnabled(TestFeatureScene1::class.java))
        Assert.assertFalse(TestFeatureScene1.initExecuted)
    }

    @Test
    fun testEnabledFeatureGroup() {
        features.enableFeatureGroups(TestFeatures.FeatureGroup1)
        Manifold.init()

        Assert.assertFalse(features.isFeatureEnabled(TestFeatureScene1::class.java))
        Assert.assertTrue(features.isFeatureEnabled(TestFeatureScene2::class.java))
        Assert.assertTrue(features.isFeatureEnabled(TestFeatureScene3::class.java))

        Assert.assertFalse(TestFeatureScene1.initExecuted)
        Assert.assertTrue(TestFeatureScene2.initExecuted)
        Assert.assertTrue(TestFeatureScene3.initExecuted)
    }

    @Test
    fun testDisabledFeatureGroup() {
        features.disableFeatureGroups(TestFeatures.FeatureGroup1)
        Manifold.init()

        Assert.assertFalse(features.isFeatureEnabled(TestFeatureScene1::class.java))
        Assert.assertFalse(features.isFeatureEnabled(TestFeatureScene2::class.java))
        Assert.assertFalse(features.isFeatureEnabled(TestFeatureScene3::class.java))

        Assert.assertFalse(TestFeatureScene1.initExecuted)
        Assert.assertFalse(TestFeatureScene2.initExecuted)
        Assert.assertFalse(TestFeatureScene3.initExecuted)
    }

    @Test
    fun testNotEnabledFeatureGroup() {
        Manifold.init()

        Assert.assertFalse(features.isFeatureEnabled(TestFeatureScene2::class.java))
        Assert.assertFalse(features.isFeatureEnabled(TestFeatureScene3::class.java))

        Assert.assertFalse(TestFeatureScene2.initExecuted)
        Assert.assertFalse(TestFeatureScene3.initExecuted)
    }

    @Test
    fun testDisabledSubFeatureGroup() {
        features.enableFeatureGroups(TestFeatures.FeatureUpperGroup1)
        features.disableFeatureGroups(TestFeatures.FeatureGroup3)
        Manifold.init()

        Assert.assertTrue(features.isFeatureEnabled(TestFeatureScene4::class.java))
        Assert.assertTrue(features.isFeatureEnabled(TestFeatureScene5::class.java))
        Assert.assertFalse(features.isFeatureEnabled(TestFeatureScene6::class.java))

        Assert.assertTrue(TestFeatureScene4.initExecuted)
        Assert.assertTrue(TestFeatureScene5.initExecuted)
        Assert.assertFalse(TestFeatureScene6.initExecuted)
    }

    @Test
    fun testFeatureSuccessor() {
        features.enableFeatureGroups(TestFeatures.FeatureGroup4)
        Manifold.init()

        Assert.assertFalse(features.isFeatureEnabled(TestFeatureScene7::class.java))
        Assert.assertTrue(features.isFeatureEnabled(TestFeatureScene8::class.java))

        Assert.assertFalse(TestFeatureScene7.initExecuted)
        Assert.assertTrue(TestFeatureScene8.initExecuted)
    }

    @Test
    fun testRunEnabledScene() {
        features.enableFeatures(TestFeatures.Feature1)
        Manifold.init()

        val r = Manifold.run(TestFeatureScene1()).get()
        Assert.assertEquals("Feature1", r)
    }

    @Test
    fun testRunNotEnabledScene() {
        Manifold.init()

        try {
            Manifold.run(TestFeatureScene1()).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e is IllegalAccessException)
        }
    }

    @Test
    fun testRunDisabledScene() {
        features.disableFeatures(TestFeatures.Feature1)
        Manifold.init()

        try {
            Manifold.run(TestFeatureScene1()).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e is IllegalAccessException)
        }
    }

    @Test
    fun testAuthFeaturePass() {
        features.enableFeatures(TestFeatures.Feature1)

        FeatureAuthenticator.configure {
            lets map TestModule.ModuleA type TestAuth.View to TestFeatures.Feature1
        }

        Manifold.authInfoProvider = mock<AuthenticateInformationProvider> {
            on { getRole(any<String>()) } doReturn CompletableFuture.completedFuture(AuthRole(permissions = arrayOf(Permission(TestModule.ModuleA, TestAuth.View, PermissionState.Allowed))))
        }

        Manifold.init()

        val r = Manifold.run(TestFeatureScene1(), "").get()
        Assert.assertEquals("Feature1", r)
    }

    @Test
    fun testAuthFeatureDeny() {
        features.enableFeatures(TestFeatures.Feature1, TestFeatures.Feature2)

        FeatureAuthenticator.configure {
            lets map TestModule.ModuleA type TestAuth.View to TestFeatures.Feature1
            lets map TestModule.ModuleA type TestAuth.Edit to TestFeatures.Feature2
        }

        Manifold.authInfoProvider = mock<AuthenticateInformationProvider> {
            on { getRole(any<String>()) } doReturn CompletableFuture.completedFuture(AuthRole(permissions = arrayOf(Permission(TestModule.ModuleA, TestAuth.View, PermissionState.Allowed))))
        }

        Manifold.init()

        try {
            val r = Manifold.run(TestFeatureScene2(), "").get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is InterceptorException)
        }
    }
}