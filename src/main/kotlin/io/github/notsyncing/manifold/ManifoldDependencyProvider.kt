package io.github.notsyncing.manifold

interface ManifoldDependencyProvider {
    fun reset()

    fun <T> get(type: Class<T>): T {
        return get(type, false)
    }

    fun <T> get(type: Class<T>, singleton: Boolean = false): T

    fun register(obj: Any)

    fun <T: S, S> registerAs(obj: T, type: Class<S>)

    fun <T: S, S> registerMapping(instClass: Class<T>, intfClass: Class<S>)

    fun registerSingleton(type: Class<*>)
}