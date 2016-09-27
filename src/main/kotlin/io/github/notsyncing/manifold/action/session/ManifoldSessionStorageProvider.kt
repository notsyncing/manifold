package io.github.notsyncing.manifold.action.session

interface ManifoldSessionStorageProvider {
    fun <T> get(sessionIdentifier: String, key: String): T?

    fun <T> put(sessionIdentifier: String, key: String, value: Any): T?

    fun <T> remove(sessionIdentifier: String, key: String): T?

    fun has(sessionIdentifier: String, key: String): Boolean

    fun clear(sessionIdentifier: String)

    fun clear()
}