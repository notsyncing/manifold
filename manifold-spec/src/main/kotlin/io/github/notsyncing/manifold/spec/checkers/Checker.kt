package io.github.notsyncing.manifold.spec.checkers

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.notsyncing.manifold.spec.models.SceneSpec
import io.github.notsyncing.manifold.spec.testcase.TestCaseInfo
import java.util.concurrent.CompletableFuture

abstract class Checker(protected val scene: SceneSpec) {
    protected val scanner: FastClasspathScanner
        get() = SpecChecker.scanner

    abstract fun checkMetadata()
    abstract fun checkCase(case: TestCaseInfo): CompletableFuture<Unit>
}