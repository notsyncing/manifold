package io.github.notsyncing.manifold.spec

import io.github.notsyncing.manifold.spec.checkers.SpecChecker
import io.github.notsyncing.manifold.spec.models.ModuleInfo

abstract class ManifoldSpecification {
    companion object {
        var modules: List<ModuleInfo> = emptyList()
    }

    abstract fun spec(): SpecBuilder

    fun specification(spec: SpecBuilder.() -> Unit): SpecBuilder {
        val b = SpecBuilder()
        b.spec()

        return b
    }

    fun checkMetadata() {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        SpecChecker(modules).runMetadata()
    }

    fun checkMetadata(sceneName: String) {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        SpecChecker(modules).runMetadata(sceneName)
    }

    fun checkCases() {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        SpecChecker(modules).runCases()
    }

    fun checkCase(sceneCase: String) {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        SpecChecker(modules).runCase(sceneCase)
    }

    fun checkScene(sceneName: String) {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        SpecChecker(modules).runScene(sceneName)
    }
}