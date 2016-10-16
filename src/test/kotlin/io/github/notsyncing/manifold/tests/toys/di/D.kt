package io.github.notsyncing.manifold.tests.toys.di

import io.github.notsyncing.manifold.di.AutoProvide

class D {
    var b: B? = null
    var c: C? = null

    @AutoProvide
    constructor(b: B, c: C) {
        this.b = b
        this.c = c
    }
}