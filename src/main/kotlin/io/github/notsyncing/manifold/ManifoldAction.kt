package io.github.notsyncing.manifold

import kotlinx.coroutines.async
import java.util.concurrent.CompletableFuture

abstract class ManifoldAction<T, R>(private var useTrans: Boolean = true,
                                    private var autoCommit: Boolean = true,
                                    private var transClass: Class<T>) {
    var transaction: ManifoldTransaction<T>? = null

    fun with(trans: ManifoldTransaction<T>?): ManifoldAction<T, R> {
        transaction = trans
        return this
    }

    fun <A: ManifoldAction<T, R>> execute(f: (A) -> CompletableFuture<R>) = async<R> {
        if ((useTrans) && (Manifold.transactionProvider == null) && (transaction == null)) {
            throw RuntimeException("Action ${this@ManifoldAction.javaClass} wants to use transaction, but no transaction provider, nor a transaction is provided!")
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
            val r = await(f(this@ManifoldAction as A))

            if (useTrans) {
                if ((!autoCommit) && (!inTrans)) {
                    await(transaction!!.commit())
                }

                if (!inTrans) {
                    //await(transaction!!.end())
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