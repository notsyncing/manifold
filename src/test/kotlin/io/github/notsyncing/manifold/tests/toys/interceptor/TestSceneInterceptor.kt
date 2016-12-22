package io.github.notsyncing.manifold.tests.toys.interceptor

import io.github.notsyncing.manifold.action.interceptors.*
import io.github.notsyncing.manifold.tests.toys.TestSceneSimple
import java.util.concurrent.CompletableFuture

@ForScenes(TestSceneSimple::class)
@ForScenesAnnotatedWith(TestSceneInterceptorAnno::class)
class TestSceneInterceptor : SceneInterceptor() {
    companion object {
        var context: SceneInterceptorContext? = null
        var forAnno: Annotation? = null
        var beforeCalled = false
        var afterCalled = false
        var destroyCalled = false

        var makeStop = false
        var changeResult = false

        fun reset() {
            context = null
            beforeCalled = false
            afterCalled = false
            destroyCalled = false

            makeStop = false
            changeResult = false
        }
    }

    override fun before(context: SceneInterceptorContext): CompletableFuture<Unit> {
        Companion.context = context
        beforeCalled = true

        if (context.annotation != null) {
            forAnno = context.annotation
        }

        if (makeStop) {
            context.interceptorResult = InterceptorResult.Stop
        }

        return super.before(context)
    }

    override fun after(context: SceneInterceptorContext): CompletableFuture<Unit> {
        Companion.context = context
        afterCalled = true

        if (changeResult) {
            context.result = "Changed"
        }

        return super.after(context)
    }

    override fun destroy(context: SceneInterceptorContext): CompletableFuture<Unit> {
        destroyCalled = true

        return super.destroy(context)
    }
}