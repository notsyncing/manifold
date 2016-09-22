package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import kotlinx.coroutines.async
import java.util.concurrent.CompletableFuture

abstract class ManifoldAction<T, R, F, C: ManifoldRunnerContext>(private var useTrans: Boolean = true,
                                                                 private var autoCommit: Boolean = true,
                                                                 private var transClass: Class<T>) {
    var transaction: ManifoldTransaction<T>? = null
    var context: C? = null

    abstract fun core(): F?

    fun with(context: C?): ManifoldAction<T, R, F, C> {
        this.context = context
        return this
    }

    fun with(trans: ManifoldTransaction<*>?): ManifoldAction<T, R, F, C> {
        transaction = trans as ManifoldTransaction<T>?
        return this
    }

    fun <A: ManifoldAction<*, R, *, *>> execute(f: (A) -> CompletableFuture<R>) = async<R> {
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