package io.github.notsyncing.manifold

import kotlinx.coroutines.async
import java.util.concurrent.CompletableFuture

abstract class ManifoldAction<T, R>(private var autoCommit: Boolean = true,
                                    private var transClass: Class<T>) {
    var transaction: ManifoldTransaction<T>? = null

    fun with(trans: ManifoldTransaction<T>?): ManifoldAction<T, R> {
        transaction = trans
        return this
    }

    fun <A: ManifoldAction<T, R>> execute(f: (A) -> CompletableFuture<R>) = async<R> {
        var inTrans = false

        if (transaction == null) {
            transaction = Manifold.transactionProvider!!.get(transClass)
        } else {
            inTrans = true
        }

        await(transaction!!.begin(!autoCommit))

        try {
            val r = await(f(this@ManifoldAction as A))

            if ((!autoCommit) && (!inTrans)) {
                await(transaction!!.commit())
            }

            if (!inTrans) {
                //await(transaction!!.end())
            }

            return@async r
        } catch (e: Exception) {
            if (!inTrans) {
                await(transaction!!.end())
            }

            throw e
        }
    }
}