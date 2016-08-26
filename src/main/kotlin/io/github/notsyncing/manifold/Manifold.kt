package io.github.notsyncing.manifold

import io.github.notsyncing.manifold.di.ManifoldDependencyInjector
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.vertx.core.Vertx
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object Manifold {
    var dependencyProvider: ManifoldDependencyProvider? = null
    var transactionProvider: ManifoldTransactionProvider? = null
    var actionPool = ConcurrentHashMap<Class<*>, ManifoldAction<*, *, *>>()

    private var vertx: Vertx? = null

    fun init() {
        if (dependencyProvider == null) {
            dependencyProvider = ManifoldDependencyInjector()
        }

        vertx = Vertx.vertx()

        ManifoldEventBus.init(vertx!!)
    }

    fun destroy(): CompletableFuture<Void> {
        reset()

        return ManifoldEventBus.stop().thenCompose {
            val c = CompletableFuture<Void>()

            if (vertx == null) {
                c.complete(null)
            } else {
                vertx?.close {
                    if (it.failed()) {
                        c.completeExceptionally(it.cause())
                    } else {
                        vertx = null
                        c.complete(it.result())
                    }
                }
            }

            return@thenCompose c
        }
    }

    fun reset() {
        dependencyProvider = null

        actionPool.clear()
    }

    fun beginTran() = transactionProvider?.get()

    fun <A: ManifoldAction<*, *, *>> registerAction(action: A) {
        actionPool[action.javaClass] = action
    }

    fun <A: ManifoldAction<*, *, *>> registerAction(actionClass: Class<A>) {
        getAction(actionClass)
    }

    fun <A: ManifoldAction<*, *, *>> getAction(action: Class<A>): A {
        val a: A

        if (!actionPool.containsKey(action)) {
            if (dependencyProvider != null) {
                a = dependencyProvider!!.get(action)
            } else {
                a = action.newInstance()
            }

            actionPool[action] = a
        } else {
            a = actionPool[action]!! as A
        }

        return a
    }

    fun <A: ManifoldAction<*, R, *>, R> run(actionClass: Class<A>, trans: ManifoldTransaction<*>? = null, f: (A) -> CompletableFuture<R>): CompletableFuture<R> {
        try {
            return getAction(actionClass).with(trans).execute(f)
        } catch (e: Exception) {
            val c = CompletableFuture<R>()
            c.completeExceptionally(e)

            return c
        }
    }

    fun <A: ManifoldAction<*, R, *>, R> run(f: (ManifoldRunner) -> CompletableFuture<R>): CompletableFuture<R> {
        val t = transactionProvider?.get()
        val runner = ManifoldRunner(t)

        return f(runner)
    }
}