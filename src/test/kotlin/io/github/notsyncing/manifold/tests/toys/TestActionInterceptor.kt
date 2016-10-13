package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.interceptors.*
import java.util.concurrent.CompletableFuture

@ForActions(TestActionSimple::class)
@ForActionsAnnotatedWith(TestActionInterceptorAnno::class)
class TestActionInterceptor : ActionInterceptor {
    companion object {
        var context: ActionInterceptorContext? = null
        var beforeCalled = false
        var afterCalled = false

        var makeStop = false
        var changeResult = false

        fun reset() {
            context = null
            beforeCalled = false
            afterCalled = false

            makeStop = false
            changeResult = false
        }
    }

    override fun before(context: ActionInterceptorContext): CompletableFuture<Unit> {
        TestActionInterceptor.context = context
        TestActionInterceptor.beforeCalled = true

        if (makeStop) {
            context.interceptorResult = InterceptorResult.Stop
        }

        return super.before(context)
    }

    override fun after(context: ActionInterceptorContext): CompletableFuture<Unit> {
        TestActionInterceptor.context = context
        TestActionInterceptor.afterCalled = true

        if (changeResult) {
            context.result = "Changed"
        }

        return super.after(context)
    }
}