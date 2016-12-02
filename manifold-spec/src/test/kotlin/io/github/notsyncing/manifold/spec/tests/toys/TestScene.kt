package io.github.notsyncing.manifold.spec.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.authenticate.SpecialAuth
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.manifold.spec.annotations.SceneMetadata
import java.util.concurrent.CompletableFuture

@SceneMetadata("新增产品数据场景")
@Feature("ProductData", groups = arrayOf("TestGroup"), defaultSpecialAuths = arrayOf(SpecialAuth.LoginOnly), internal = false)
class TestScene(private val name: String,
                private val details: String?) :
        ManifoldScene<OperationResult>(enableEventNode = false) {
    constructor() : this("", null)

    override fun stage(): CompletableFuture<OperationResult> {
        return CompletableFuture.completedFuture(OperationResult.Success)
    }
}