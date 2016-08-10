package io.github.notsyncing.manifold

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object Manifold {
    var dependencyProvider: ManifoldDependencyProvider? = null
    var transactionProvider: ManifoldTransactionProvider? = null
    var actionPool: MutableMap<Class<*>, ManifoldAction<*, *>> = ConcurrentHashMap()

    fun reset() {
        dependencyProvider = null

        actionPool.clear()
    }

    fun <A: ManifoldAction<T, R>, T, R> registerAction(action: A) {
        actionPool[action.javaClass] = action
    }

    fun <A: ManifoldAction<T, R>, T, R> getAction(action: Class<A>): A {
        val a: A

        if (!actionPool.containsKey(action)) {
            if (dependencyProvider != null) {
                a = dependencyProvider!!.get(action)!!
            } else {
                a = action.newInstance()
            }

            actionPool[action] = a
        } else {
            a = actionPool[action]!! as A
        }

        return a
    }

    fun <A: ManifoldAction<T, R>, T, R> run(type: Class<A>, trans: ManifoldTransaction<T>? = null, f: (A) -> CompletableFuture<R>): CompletableFuture<R> {
        return getAction(type).with(trans).execute(f as (ManifoldAction<T, R>) -> CompletableFuture<R>)
    }
}