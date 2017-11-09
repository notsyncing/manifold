import io.github.notsyncing.manifold.story.drama.DramaScriptLifecycle
import io.github.notsyncing.manifold.story.drama.engine.kotlin.Role
import io.github.notsyncing.manifold.story.drama.engine.kotlin.lifecycle
import io.github.notsyncing.manifold.story.tests.DramaKotlinScriptTest
import io.github.notsyncing.manifold.story.tests.toys.TestFunctions
import java.util.concurrent.CompletableFuture

val user = Role()

user.on("simpleActionKt") { context, params, permParams ->
    CompletableFuture.completedFuture("Hello, world!")
}

user.on<Unit>("exceptionInKotlinActionKt") { context, params, permParams ->
    throw RuntimeException("EXCEPTION")
}

user.on("exceptionInJavaActionKt") { context, params, permParams ->
    CompletableFuture.completedFuture(TestFunctions.exception())
}

user.on("nestedCFPAction1Kt") { context, params, permParams ->
    TestFunctions.successCf()
            .thenCompose {
                TestFunctions.failedCf()
            }
            .thenApply {  }
}

val authUser = Role("TestModule", "TestAuth")

authUser.on("simpleAuthActionKt") { context, params, permParams ->
    CompletableFuture.completedFuture("Hello, user!")
}

lifecycle(object : DramaScriptLifecycle {
    override fun afterEvaluate() {
        DramaKotlinScriptTest.afterEval = true
    }

    override fun beforeDestroy() {
    }
})