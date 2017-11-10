package io.github.notsyncing.manifold.spec.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.feature.Feature
import java.util.concurrent.CompletableFuture

@SceneMetadata("测试场景4F")
@Feature("TestScene4F", groups = arrayOf("TestGroup"), internal = false)
class TestScene4F() : ManifoldScene<String>(enableEventNode = false) {
    override fun stage(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("SUCCESS")
    }
}