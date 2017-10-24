package io.github.notsyncing.manifold

import io.github.notsyncing.manifold.action.ManifoldTransaction

interface ManifoldTransactionProvider {
    @Deprecated("Implement fun get(startStack: Exception) instead")
    fun get(): ManifoldTransaction<*> {
        throw UnsupportedOperationException("Not implemented")
    }

    fun get(startStack: Exception): ManifoldTransaction<*> {
        return get()
    }

    fun <T> get(transClass: Class<T>, startStack: Exception): ManifoldTransaction<T> {
        return get(startStack) as ManifoldTransaction<T>
    }
}