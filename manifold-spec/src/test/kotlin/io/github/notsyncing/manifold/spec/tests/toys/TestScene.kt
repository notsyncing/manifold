package io.github.notsyncing.manifold.spec.tests.toys

import io.github.notsyncing.manifold.action.ActionMetadata
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.authenticate.SpecialAuth
import io.github.notsyncing.manifold.feature.Feature
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import java.util.concurrent.CompletableFuture

@ActionMetadata("获取当前公司ID")
class GetCurrentCompanyIdAction(private val token: String) : ManifoldAction<Long>() {
    override fun action(): CompletableFuture<Long> {
        if (token == "FAIL") {
            return CompletableFuture.completedFuture(0)
        }

        return CompletableFuture.completedFuture(100)
    }
}

@ActionMetadata("新增产品数据")
class AddCompanyAction(private val name: String, private val details: String?) : ManifoldAction<OperationResult>() {
    override fun action(): CompletableFuture<OperationResult> {
        return CompletableFuture.completedFuture(OperationResult.Success)
    }
}

@SceneMetadata("新增产品数据场景")
@Feature("ProductData", groups = arrayOf("TestGroup"), defaultSpecialAuths = arrayOf(SpecialAuth.LoginOnly), internal = false)
class TestScene(private val name: String,
                private val details: String?) :
        ManifoldScene<OperationResult>(enableEventNode = false) {
    constructor() : this("", null)

    override fun stage() = async<OperationResult> {
        if (name.isEmpty()) {
            return@async OperationResult.Failed
        }

        val companyId = m(GetCurrentCompanyIdAction(sessionIdentifier ?: "")).await()

        if (companyId <= 0) {
            return@async OperationResult.Failed
        }

        val r = m(AddCompanyAction(name, details)).await()

        if (r == OperationResult.Failed) {
            return@async r
        } else {
            OperationResult.Success
        }
    }
}