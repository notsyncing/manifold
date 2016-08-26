package io.github.notsyncing.manifold

interface ManifoldTransactionProvider {
    fun get(): ManifoldTransaction<*>

    fun <T> get(transClass: Class<T>): ManifoldTransaction<T> {
        return get() as ManifoldTransaction<T>
    }
}