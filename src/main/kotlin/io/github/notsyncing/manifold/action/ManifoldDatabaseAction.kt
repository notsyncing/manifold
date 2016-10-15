package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.storage.ManifoldStorage
import kotlinx.coroutines.async
import java.util.concurrent.CompletableFuture

abstract class ManifoldDatabaseAction<T, R>(private var transClass: Class<T>) : ManifoldAction<R>() {
    protected var transaction: ManifoldTransaction<T>? = null

    override fun provideDependency(t: Class<*>): Any? {
        if (ManifoldStorage::class.java.isAssignableFrom(t)) {
            val o = t.newInstance()
            storageList.add(o as ManifoldStorage<*>)
            return o
        } else {
            return super.provideDependency(t)
        }
    }

    override fun <A: ManifoldAction<R>> execute(f: (A) -> CompletableFuture<R>) = async<R> {
        if (Manifold.transactionProvider == null) {
            throw RuntimeException("Action ${this@ManifoldDatabaseAction.javaClass} wants to use transaction, but no transaction provider found!")
        }

        if (context.transaction == null) {
            context.transaction = Manifold.transactionProvider!!.get(transClass)
        }

        transaction = context.transaction as ManifoldTransaction<T>?

        storageList.forEach { it.sceneContext = context }

        await(context.transaction!!.begin(!context.autoCommit))

        try {
            val r = await(super.execute(f))

            if (context.autoCommit) {
                await(context.transaction!!.end())
            }

            return@async r
        } catch (e: Exception) {
            await(context.transaction!!.end())

            throw e
        }
    }
}