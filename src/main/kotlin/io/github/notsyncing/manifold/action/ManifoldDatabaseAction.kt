package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.storage.ManifoldStorage
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture

abstract class ManifoldDatabaseAction<T, R>(private var transClass: Class<T>) : ManifoldAction<R>() {
    protected var transaction: ManifoldTransaction<T>? = null

    override fun provideDependency(t: Class<*>): Any? {
        if (ManifoldStorage::class.java.isAssignableFrom(t)) {
            val o = super.provideDependency(t)

            if (o != null) {
                storageList.add(o as ManifoldStorage<*>)
            }

            return o
        } else {
            return super.provideDependency(t)
        }
    }

    override fun <A: ManifoldAction<R>> execute(f: (A) -> CompletableFuture<R>) = future<R> {
        if (Manifold.transactionProvider == null) {
            throw RuntimeException("Action ${this@ManifoldDatabaseAction.javaClass} wants to use transaction, but no transaction provider found!")
        }

        if (context.transaction == null) {
            context.transaction = Manifold.transactionProvider!!.get(transClass)
        }

        transaction = context.transaction as ManifoldTransaction<T>?

        storageList.forEach { it.sceneContext = context }

        context.transaction!!.begin(!context.autoCommit).await()

        try {
            val r = super.execute(f).await()

            if (context.autoCommit) {
                context.transaction!!.end().await()
                context.transaction = null
            }

            return@future r
        } catch (e: Exception) {
            e.printStackTrace()

            context.transaction!!.end().await()
            context.transaction = null

            throw e
        }
    }
}