package io.github.notsyncing.manifold.spec.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.Probe
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.feature.Feature
import kotlinx.coroutines.experimental.future.future

@SceneMetadata("测试场景8")
@Feature("TestScene8", groups = arrayOf("TestGroup"), internal = false)
class TestScene8() : ManifoldScene<String>(enableEventNode = false) {
    @Probe
    private val probe: Int = 2

    override fun stage() = future {
        "3"
    }
}