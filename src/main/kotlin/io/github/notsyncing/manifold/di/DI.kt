package io.github.notsyncing.manifold.di

import io.github.notsyncing.manifold.Manifold

inline fun <reified T> di(provides: Array<*> = emptyArray<Any>()): T? {
    return Manifold.dependencyProvider?.get(T::class.java, provides = provides)
}

inline fun <reified T> dip(vararg provides: Any?): T? {
    return di(provides)
}

inline fun <reified T> dis(provides: Array<*> = emptyArray<Any>()): T {
    val o = Manifold.dependencyProvider?.get(T::class.java, provides = provides)

    if (o == null) {
        throw ClassNotFoundException("${T::class.java} cannot be injected, maybe not found?")
    }

    return o
}

inline fun <reified T> dips(vararg provides: Any?): T {
    return dis(provides)
}

inline fun <reified T: I, reified I> diReg() {
    Manifold.dependencyProvider?.registerMapping(T::class.java, I::class.java)
}

inline fun <reified T> diUnreg() {
    Manifold.dependencyProvider?.unregisterMapping(T::class.java)
}
