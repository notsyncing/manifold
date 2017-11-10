package io.github.notsyncing.manifold.spec.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.feature.FeatureAuthenticator
import io.github.notsyncing.manifold.spec.tests.toys.Auth
import io.github.notsyncing.manifold.spec.tests.toys.Module
import io.github.notsyncing.manifold.spec.tests.toys.TestSpec
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class SceneCheckerTest {
    @Ignore
    companion object {
        val spec = TestSpec()

        @BeforeClass
        @JvmStatic
        fun setUp() {
            FeatureAuthenticator.configure {
                our feature "TestScene2" needs Module.ProductData type Auth.View
                our feature "ProductData" needs Module.ProductData type Auth.Edit
            }

            Manifold.features.enableFeatureGroups("TestGroup")

            Manifold.init()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            Manifold.reset()
        }
    }

    @RunWith(Parameterized::class)
    class SceneMetadataTest(private val sceneName: String) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{index}: {0}")
            fun data(): Collection<Array<Any>> {
                return spec.getSceneCasesList()
                        .filter { (_, scene) -> !scene.skip }
                        .map { (_, scene) -> scene.name }
                        .distinct()
                        .map { arrayOf<Any>(it) }
            }
        }

        @Test
        fun test() {
            spec.checkMetadata(sceneName)
        }
    }

    @RunWith(Parameterized::class)
    class SceneCasesTest(private val sceneName: String,
                         private val caseName: String) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{index}: {0} {1}")
            fun data(): List<Array<String>> {
                return spec.getSceneCasesList()
                        .filter { (_, scene) -> !scene.skip }
                        .filter { (case, _) -> case?.skip == false }
                        .filter { (case, _) -> case?.behavior?.isNotEmpty() == true }
                        .map { (case, scene) -> arrayOf(scene.name, case!!.behavior) }
            }
        }

        @Test
        fun test() {
            spec.checkCase(sceneName, caseName)
        }
    }
}