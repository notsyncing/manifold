package io.github.notsyncing.manifold.spec.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.feature.Feature
import java.util.concurrent.CompletableFuture

@SceneMetadata("测试场景2")
@Feature("TestScene2", groups = arrayOf("TestGroup"), internal = false)
class TestScene2() : ManifoldScene<OperationResult>(enableEventNode = false) {
    override fun stage(): CompletableFuture<OperationResult> {
        return CompletableFuture.completedFuture(OperationResult.Success)
    }
}