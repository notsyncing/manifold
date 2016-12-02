package io.github.notsyncing.manifold.spec.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.feature.FeatureAuthenticator
import io.github.notsyncing.manifold.spec.tests.toys.Auth
import io.github.notsyncing.manifold.spec.tests.toys.Module
import io.github.notsyncing.manifold.spec.tests.toys.TestSpec
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.ComparisonFailure
import org.junit.Test

class SceneCheckerTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            FeatureAuthenticator.configure {
                our feature "ProductData" needs Module.ProductData type Auth.Edit
                our feature "TestScene2" needs Module.ProductData type Auth.Edit
            }

            Manifold.init()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            Manifold.reset()
        }
    }

    @Test
    fun testMetadata() {
        val spec = TestSpec()
        spec.checkMetadata("新增产品数据场景")
    }

    @Test
    fun testMetadataWithWrongFeatureName() {
        val spec = TestSpec()

        try {
            spec.checkMetadata("测试场景1")
            assertTrue(false)
        } catch (e: ComparisonFailure) {
            assertTrue(e.message!!.contains("wrong feature name"))
        }
    }

    @Test
    fun testMetadataWithWrongPermission() {
        val spec = TestSpec()

        try {
            spec.checkMetadata("测试场景2")
            assertTrue(false)
        } catch (e: AssertionError) {
            assertTrue(e.message!!.contains("wrong permission type"))
        }
    }

    @Test
    fun testMetadataWithWrongParameterName() {
        val spec = TestSpec()

        try {
            spec.checkMetadata("测试场景3")
            assertTrue(false)
        } catch (e: AssertionError) {
            assertTrue(e.message!!.contains("matches the parameters"))
        }
    }

    @Test
    fun testMetadataWithWrongReturnType() {
        val spec = TestSpec()

        try {
            spec.checkMetadata("测试场景4")
            assertTrue(false)
        } catch (e: AssertionError) {
            assertTrue(e.message!!.contains("wrong return type"))
        }
    }
}