package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.di.NoProvide
import io.github.notsyncing.manifold.di.ProvideAsSingleton

@ProvideAsSingleton
class E(val a: A, val b: B, @NoProvide val c: C?) {
    companion object {
        var counter = 0
    }

    init {
        counter++
    }
}