package io.github.notsyncing.manifold

interface ManifoldManagerProvider {
    fun <T> get(type: Class<T>): T?
}