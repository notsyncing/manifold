package io.github.notsyncing.manifold.spec.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.manifold.spec.tests.toys.actions.TestActionA
import io.github.notsyncing.manifold.spec.tests.toys.actions.TestActionB
import io.github.notsyncing.manifold.spec.tests.toys.actions.TestActionC
import kotlinx.coroutines.async
import kotlinx.coroutines.await

@SceneMetadata("测试场景7")
@Feature("TestScene7", groups = arrayOf("TestGroup"), internal = false)
class TestScene7(private val cond: Boolean) : ManifoldScene<String>(enableEventNode = false) {
    constructor() : this(false)

    override fun stage() = async {
        val r: String

        if (cond) {
            r = m(TestActionA()).await()
        } else {
            r = m(TestActionB()).await()
        }

        m(TestActionC()).await()

        r
    }
}