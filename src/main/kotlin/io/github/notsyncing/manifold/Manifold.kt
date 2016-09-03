package io.github.notsyncing.manifold

import io.github.notsyncing.manifold.action.*
import io.github.notsyncing.manifold.di.ManifoldDependencyInjector
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.vertx.core.Vertx
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object Manifold {
    var dependencyProvider: ManifoldDependencyProvider? = null
    var transactionProvider: ManifoldTransactionProvider? = null
    var actionPool = ConcurrentHashMap<Class<*>, ManifoldAction<*, *, *, *>>()

    private var vertx: Vertx? = null

    fun init() {
        if (dependencyProvider == null) {
            dependencyProvider = ManifoldDependencyInjector()
        }

        vertx = Vertx.vertx()

        ManifoldEventBus.init(vertx!!)

        processScenes()
    }

    private fun processScenes() {
        dependencyProvider?.getAllSubclasses(ManifoldScene::class.java) {
            try {
                it.newInstance().init()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

        ManifoldScene.reset()
    }

    fun beginTran() = transactionProvider?.get()

    fun <A: ManifoldAction<*, *, *, *>> registerAction(action: A) {
        actionPool[action.javaClass] = action
    }

    fun <A: ManifoldAction<*, *, *, *>> registerAction(actionClass: Class<A>) {
        getAction(actionClass)
    }

    inline fun <reified A: ManifoldAction<*, *, *, *>> registerAction() = Manifold.registerAction(A::class.java)

    fun <A: ManifoldAction<*, *, *, *>> getAction(action: Class<A>): A {
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

    inline fun <reified A: ManifoldAction<*, *, *, *>> getAction(): A = getAction(A::class.java)

    fun <A: ManifoldAction<*, R, *, C>, R, C: ManifoldRunnerContext> run(actionClass: Class<A>,
                                                                         trans: ManifoldTransaction<*>? = null,
                                                                         context: C? = null,
                                                                         f: (A) -> CompletableFuture<R>): CompletableFuture<R> {
        try {
            return getAction(actionClass).with(context).with(trans).execute(f)
        } catch (e: Exception) {
            val c = CompletableFuture<R>()
            c.completeExceptionally(e)

            return c
        }
    }

    fun <R> run(context: ManifoldRunnerContext? = null,
                f: (ManifoldRunner) -> CompletableFuture<R>): CompletableFuture<R> {
        val t = transactionProvider?.get()
        val runner = ManifoldRunner(t, context)

        return f(runner)
    }

    fun <R> run(scene: ManifoldScene<R>): CompletableFuture<R> {
        return run { m ->
            scene.m = m
            scene.stage()
        }
    }
}