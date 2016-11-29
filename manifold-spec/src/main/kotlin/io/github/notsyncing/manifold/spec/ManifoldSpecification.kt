package io.github.notsyncing.manifold.spec

abstract class ManifoldSpecification {
    abstract fun spec(): SpecBuilder

    protected fun specification(spec: SpecBuilder.() -> Unit): SpecBuilder {
        val b = SpecBuilder()
        b.spec()

        return b
    }

    fun check() {
        val s = spec()


    }
}