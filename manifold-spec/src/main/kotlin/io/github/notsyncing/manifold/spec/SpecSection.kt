package io.github.notsyncing.manifold.spec

abstract class SpecSection {
    abstract fun spec(): SpecBuilder

    protected fun specification(spec: SpecBuilder.() -> Unit): SpecBuilder {
        val b = SpecBuilder()
        b.spec()

        return b
    }
}