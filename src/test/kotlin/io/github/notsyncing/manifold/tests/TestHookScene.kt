package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.tests.toys.hooks.TestHook
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future

class TestHookScene : ManifoldScene<Int>() {
    override fun stage() = future {
        var i = 2

        i = hook(TestHook::class.java, null, i).await() ?: -5

        i++

        i
    }
}