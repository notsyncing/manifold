package io.github.notsyncing.manifold

interface ManifoldDependencyProvider {
    fun <T> get(type: Class<T>): T?
}