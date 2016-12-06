package io.github.notsyncing.manifold.spec.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.feature.Feature
import java.util.concurrent.CompletableFuture

@SceneMetadata("测试场景3")
@Feature("TestScene3", groups = arrayOf("TestGroup"), internal = false)
class TestScene3(private val id: Int) : ManifoldScene<OperationResult>(enableEventNode = false) {
    constructor() : this(0)

    override fun stage(): CompletableFuture<OperationResult> {
        return CompletableFuture.completedFuture(OperationResult.Success)
    }
}