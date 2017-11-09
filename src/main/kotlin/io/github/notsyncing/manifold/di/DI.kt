package io.github.notsyncing.manifold.di

import io.github.notsyncing.manifold.Manifold

inline fun <reified T> di(): T? {
    return Manifold.dependencyProvider?.get(T::class.java)
}

inline fun <reified T> dis(): T {
    val o = Manifold.dependencyProvider?.get(T::class.java)

    if (o == null) {
        throw ClassNotFoundException("${T::class.java} cannot be injected, maybe not found?")
    }

    return o
}

inline fun <reified T: I, reified I> diReg() {
    Manifold.dependencyProvider?.registerMapping(T::class.java, I::class.java)
}

inline fun <reified T> diUnreg() {
    Manifold.dependencyProvider?.unregisterMapping(T::class.java)
}
