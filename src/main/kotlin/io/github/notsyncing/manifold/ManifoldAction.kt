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

    fun execute(f: (ManifoldAction<T, R>) -> CompletableFuture<R>) = async<R> {
        val trans: ManifoldTransaction<T>

        if (transaction == null) {
            trans = Manifold.transactionProvider!!.get(transClass)
        } else {
            trans = transaction!!
        }

        await(trans.begin(!autoCommit))

        try {
            val r = await(f(this@ManifoldAction))

            if ((!autoCommit) && (transaction == null)) {
                await(trans.commit())
            }

            if (transaction == null) {
                await(trans.end())
            }

            return@async r
        } catch (e: Exception) {
            if (transaction == null) {
                await(trans.end())
            }

            throw e
        }
    }
}