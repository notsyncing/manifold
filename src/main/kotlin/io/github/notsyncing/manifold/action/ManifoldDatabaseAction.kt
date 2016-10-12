package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import kotlinx.coroutines.async
import java.util.concurrent.CompletableFuture

abstract class ManifoldDatabaseAction<T, R>(private var useTrans: Boolean = true,
                                            private var autoCommit: Boolean = true,
                                            private var transClass: Class<T>) : ManifoldAction<R>() {
    protected var transaction: ManifoldTransaction<T>? = null

    override fun withTransaction(trans: ManifoldTransaction<*>?): ManifoldAction<R> {
        transaction = trans as ManifoldTransaction<T>?
        return super.withTransaction(trans)
    }

    override fun <A: ManifoldAction<R>> execute(f: (A) -> CompletableFuture<R>) = async<R> {
        if ((useTrans) && (Manifold.transactionProvider == null) && (transaction == null)) {
            throw RuntimeException("Action ${this@ManifoldDatabaseAction.javaClass} wants to use transaction, but no transaction provider, nor a transaction is provided!")
        }

        var inTrans = false

        if (useTrans) {
            if (transaction == null) {
                transaction = Manifold.transactionProvider!!.get(transClass)
            } else {
                inTrans = true
            }

            if (useTrans) {
                await(transaction!!.begin(!autoCommit))
            }
        }

        try {
            val r = await(super.execute(f))

            if (useTrans) {
                if ((!autoCommit) && (!inTrans)) {
                    await(transaction!!.commit())
                }

                if (!inTrans) {
                    await(transaction!!.end())
                }
            }

            return@async r
        } catch (e: Exception) {
            if ((useTrans) && (!inTrans)) {
                await(transaction!!.end())
            }

            throw e
        }
    }
}