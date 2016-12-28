package io.github.notsyncing.manifold.spec.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.feature.Feature
import java.util.concurrent.CompletableFuture

@SceneMetadata("测试场景3A")
@Feature("TestScene3A", groups = arrayOf("TestGroup"), internal = false)
class TestScene3A(private val id: Int,
                  private val nullableParam: Int?) : ManifoldScene<OperationResult>(enableEventNode = false) {
    constructor() : this(0, null)

    override fun stage(): CompletableFuture<OperationResult> {
        return CompletableFuture.completedFuture(OperationResult.Success)
    }
}