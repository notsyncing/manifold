package io.github.notsyncing.manifold.action

import com.alibaba.fastjson.JSON
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.InterceptorException
import io.github.notsyncing.manifold.action.interceptors.InterceptorResult
import io.github.notsyncing.manifold.action.interceptors.SceneInterceptorContext
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.InternalEvent
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.utils.DependencyProviderUtils
import kotlinx.coroutines.async
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

abstract class ManifoldScene<R>(enableEventNode: Boolean = true,
                                eventNodeId: String = "",
                                eventNodeGroups: Array<String> = emptyArray(),
                                val context: SceneContext = SceneContext()) {
    protected var event: ManifoldEvent? = null
    protected var eventNode: ManifoldEventNode? = null

    lateinit var m: ManifoldActionContextRunner

    val sessionIdentifier: String?
        get() = m.sessionIdentifier

    companion object {
        private val eventNodes = ConcurrentHashMap<Class<ManifoldScene<*>>, ManifoldEventNode>()

        fun reset() {
            eventNodes.clear()
        }
    }

    init {
        val c = this.javaClass as Class<ManifoldScene<*>>

        DependencyProviderUtils.autoProvideProperties(this, this::provideDependency)

        if (enableEventNode) {
            if (eventNodes.containsKey(c)) {
                eventNode = eventNodes[c]
            } else {
                eventNode = ManifoldEventBus.register(eventNodeId, *eventNodeGroups)
                eventNodes[c] = eventNode!!

                awakeOnEvent(InternalEvent.TransitionToScene)
            }
        }
    }

    constructor(event: ManifoldEvent) : this() {
        this.event = event
    }

    open protected fun provideDependency(t: Class<*>): Any? {
        return Manifold.dependencyProvider?.get(t)
    }

    protected fun awakeOnEvent(event: String) {
        eventNode?.on(event) {
            Manifold.run(this.javaClass, it, it.sessionId)
        }
    }

    protected fun transitionTo(targetEventGroup: String, event: String, data: Any, waitForResult: Boolean = false): CompletableFuture<ManifoldEvent?> {
        val e = ManifoldEvent(event, data)
        e.sessionId = m.sessionIdentifier ?: ""

        if (waitForResult) {
            return eventNode?.sendToAnyInGroupAndWaitForReply(targetEventGroup, e) ?: CompletableFuture.completedFuture(null as ManifoldEvent?)
        } else {
            eventNode?.sendToAnyInGroup(targetEventGroup, e)
            return CompletableFuture.completedFuture(null)
        }
    }

    protected inline fun <reified A: Any> transitionTo(targetSceneEventGroup: String, parameters: Array<Any>? = null, waitForResult: Boolean = false): CompletableFuture<A?> {
        val d = JSON.toJSONString(parameters)
        val e = ManifoldEvent(InternalEvent.TransitionToScene, d)
        e.sessionId = m.sessionIdentifier ?: ""

        if (waitForResult) {
            return eventNode?.sendToAnyInGroupAndWaitForReply(targetSceneEventGroup, e)
                    ?.thenApply { JSON.parseObject(it?.data, A::class.java) } ?: CompletableFuture.completedFuture(null as A)
        } else {
            eventNode?.sendToAnyInGroup(targetSceneEventGroup, e)
            return CompletableFuture.completedFuture(null)
        }
    }

    protected fun useTransaction() {
        context.autoCommit = false
    }

    protected fun splitTransaction() = async<Unit> {
        if ((context.transaction == null) || (context.autoCommit)) {
            return@async
        }

        await(context.transaction!!.commit(false))
    }

    protected fun runInBackground(keepTransaction: Boolean, func: () -> Unit,
                                  onException: ((Exception) -> Unit)? = null): CompletableFuture<Unit> {
        if (keepTransaction) {
            context.transactionRefCount++
        }

        val f = CompletableFuture<Unit>()
        var ex: Exception? = null

        CompletableFuture.runAsync(Runnable {
            try {
                func()
            } catch (e: Exception) {
                context.transaction!!.rollback(false).get()
                ex = e
            }
        }, Manifold.sceneBgWorkerPool).thenCompose {
            if (ex != null) {
                if (onException != null) {
                    onException(ex!!)
                }
            }

            if (keepTransaction) {
                context.transactionRefCount--

                if (context.transactionRefCount <= 0) {
                    return@thenCompose endTransaction()
                }
            }

            return@thenCompose CompletableFuture.completedFuture(Unit)
        }.thenAccept {
            if (ex == null) {
                f.complete(it)
            } else {
                f.completeExceptionally(ex)
            }
        }.exceptionally {
            f.completeExceptionally(it.cause)
            return@exceptionally null
        }

        return f
    }

    open fun init() {

    }

    abstract protected fun stage(): CompletableFuture<R>

    fun execute() = async<R> {
        context.sessionIdentifier = sessionIdentifier

        val c = this@ManifoldScene.javaClass as Class<ManifoldScene<*>>
        val interceptorClasses = Manifold.interceptors.getInterceptorsForScene(c)
        val functor = { stage() }

        if (interceptorClasses.isNotEmpty()) {
            val interceptorContext = SceneInterceptorContext(this@ManifoldScene)
            val interceptors = interceptorClasses.map { Pair(it, Manifold.dependencyProvider!!.get(it.interceptorClass)) }

            interceptors.forEach {
                val (info, i) = it

                interceptorContext.annotation = info.forAnnotation

                i!!.m = m
                i.before(interceptorContext)

                if (interceptorContext.interceptorResult == InterceptorResult.Stop) {
                    throw InterceptorException("Interceptor ${i.javaClass} stopped the execution of scene ${this@ManifoldScene.javaClass}", interceptorContext.exception)
                }
            }

            try {
                interceptorContext.result = await(functor())

                interceptors.forEach {
                    val (info, i) = it

                    interceptorContext.annotation = info.forAnnotation
                    i!!.after(interceptorContext)
                }

                await(afterExecution())
            } catch (e: Exception) {
                await(afterExecution(false))

                if (e is SceneFailedException) {
                    interceptorContext.result = e.data
                } else {
                    throw e
                }
            }

            return@async interceptorContext.result as R
        }

        var r: R

        try {
            r = await(functor())

            await(afterExecution())
        } catch (e: Exception) {
            await(afterExecution(false))

            if (e is SceneFailedException) {
                r = e.data as R
            } else {
                throw e
            }
        }

        return@async r
    }

    private fun endTransaction(commit: Boolean = true) = async<Unit> {
        if (context.transaction != null) {
            if (!context.autoCommit) {
                if (commit) {
                    await(context.transaction!!.commit())
                }

                await(context.transaction!!.end())
            }
        }
    }

    protected fun rollbackTransaction() = async<Unit> {
        if (context.transaction != null) {
            if (!context.autoCommit) {
                await(context.transaction!!.rollback(false))
            }
        }
    }

    private fun afterExecution(commit: Boolean = true) = async<Unit> {
        if (context.transactionRefCount <= 0) {
            await(endTransaction(commit))
        }
    }
}