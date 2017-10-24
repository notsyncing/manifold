package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.storage.ManifoldStorage
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture

abstract class ManifoldTransactionalAction<T, R>(private var transClass: Class<T>) : ManifoldAction<R>() {
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

    override fun <A: ManifoldAction<R>> execute(f: (A) -> CompletableFuture<R>): CompletableFuture<R> {
        if (Manifold.transactionProvider == null) {
            throw RuntimeException("Action ${this::class.java} wants to use transaction, but no transaction provider found!")
        }

        if (this.context.transaction == null) {
            this.context.transaction = Manifold.transactionProvider!!.get(transClass, executionStartStack!!)
        }

        transaction = this.context.transaction as ManifoldTransaction<T>?

        storageList.forEach { it.sceneContext = this.context }

        return future {
            this@ManifoldTransactionalAction.context.transaction!!.begin(!this@ManifoldTransactionalAction.context.autoCommit).await()

            try {
                val r = super.execute(f).await()

                if (this@ManifoldTransactionalAction.context.autoCommit) {
                    this@ManifoldTransactionalAction.context.transaction!!.end().await()
                    this@ManifoldTransactionalAction.context.transaction = null
                }

                return@future r
            } catch (e: Exception) {
                e.printStackTrace()

                this@ManifoldTransactionalAction.context.transaction!!.end().await()
                this@ManifoldTransactionalAction.context.transaction = null

                throw e
            }
        }
    }
}