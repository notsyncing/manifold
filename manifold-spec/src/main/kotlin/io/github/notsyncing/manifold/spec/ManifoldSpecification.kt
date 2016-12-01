package io.github.notsyncing.manifold.spec

import io.github.notsyncing.manifold.spec.checkers.SpecChecker

abstract class ManifoldSpecification {
    abstract fun spec(): SpecBuilder

    protected fun specification(spec: SpecBuilder.() -> Unit): SpecBuilder {
        val b = SpecBuilder()
        b.spec()

        return b
    }

    fun check() {
        val modules = spec().build()

        SpecChecker(modules).run()
    }
}