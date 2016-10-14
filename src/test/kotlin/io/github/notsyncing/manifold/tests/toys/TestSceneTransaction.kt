package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import kotlinx.coroutines.async

class TestSceneTransaction(val useTrans: Boolean = true) : ManifoldScene<String>(enableEventNode = false) {
    override fun stage() = async<String> {
        if (useTrans) {
            useTransaction()
        }

        await(m(TestAction()))

        return@async "Hello!"
    }
}