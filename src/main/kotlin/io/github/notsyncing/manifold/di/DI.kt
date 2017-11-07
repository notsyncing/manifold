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