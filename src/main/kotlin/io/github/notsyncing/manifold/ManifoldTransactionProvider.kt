package io.github.notsyncing.manifold

interface ManifoldTransactionProvider {
    fun <T> get(transClass: Class<T>): ManifoldTransaction<T>
}