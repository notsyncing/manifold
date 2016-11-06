package io.github.notsyncing.manifold.tests.toys.interceptor

import io.github.notsyncing.manifold.action.interceptors.ForEveryScene
import io.github.notsyncing.manifold.action.interceptors.SceneInterceptor
import io.github.notsyncing.manifold.action.interceptors.SceneInterceptorContext
import java.util.concurrent.CompletableFuture

@ForEveryScene
class TestGlobalSceneInterceptor : SceneInterceptor() {
    companion object {
        var context: SceneInterceptorContext? = null
        var beforeCalled = false
        var afterCalled = false

        fun reset() {
            context = null
            beforeCalled = false
            afterCalled = false
        }
    }

    override fun before(context: SceneInterceptorContext): CompletableFuture<Unit> {
        Companion.context = context
        beforeCalled = true

        return super.before(context)
    }

    override fun after(context: SceneInterceptorContext): CompletableFuture<Unit> {
        Companion.context = context
        afterCalled = true

        return super.after(context)
    }
}