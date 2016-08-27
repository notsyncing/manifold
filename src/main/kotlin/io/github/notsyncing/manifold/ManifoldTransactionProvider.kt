package io.github.notsyncing.manifold

import io.github.notsyncing.manifold.action.ManifoldTransaction

interface ManifoldTransactionProvider {
    fun get(): ManifoldTransaction<*>

    fun <T> get(transClass: Class<T>): ManifoldTransaction<T> {
        return get() as ManifoldTransaction<T>
    }
}