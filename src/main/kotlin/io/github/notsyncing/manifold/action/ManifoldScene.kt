package io.github.notsyncing.manifold.action

import com.alibaba.fastjson.JSON
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.*
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.InternalEvent
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.hooking.Hook
import io.github.notsyncing.manifold.utils.DependencyProviderUtils
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

abstract class ManifoldScene<R>(private val enableEventNode: Boolean = false,
                                private val eventNodeId: String = "",
                                private val eventNodeGroups: Array<String> = emptyArray(),
                                val context: SceneContext = SceneContext()) {
    private var succCond1: ((R) -> Boolean)? = null
    private var succCond2: (() -> R)? = null

    protected var event: ManifoldEvent? = null
    var eventNode: ManifoldEventNode? = null

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
        DependencyProviderUtils.autoProvideProperties(this, this::provideDependency)

        if (enableEventNode) {
            eventNode = eventNodes[this::class.java as Class<ManifoldScene<*>>]
        }

        context.scene = this
    }

    constructor(event: ManifoldEvent) : this() {
        this.event = event
    }

    open protected fun provideDependency(t: Class<*>): Any? {
        return Manifold.dependencyProvider?.get(t)
    }

    protected fun awakeOnEvent(event: String) {
        if (eventNode == null) {
            throw IllegalAccessException("Scene ${this::class.java} wants to be awaken on event $event, but it has no event node!")
        }

        eventNode?.on(event) {
            Manifold.run(this::class.java as Class<ManifoldScene<R>>, it, it.sessionId)
        }
    }

    protected fun transitionTo(targetEventGroup: String, event: String, data: Any, waitForResult: Boolean = false): CompletableFuture<ManifoldEvent?> {
        if (eventNode == null) {
            throw IllegalAccessException("Scene ${this::class.java} wants to transition to $targetEventGroup with event $event, but it has no event node!")
        }

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

    fun useTransaction() {
        context.autoCommit = false
    }

    fun splitTransaction() = future {
        if ((this@ManifoldScene.context.transaction == null) || (this@ManifoldScene.context.autoCommit)) {
            return@future
        }

        this@ManifoldScene.context.transaction!!.commit(false).await()
    }

    protected fun runInBackground(keepTransaction: Boolean, func: () -> Unit,
                                  onException: ((Exception) -> Unit)?): CompletableFuture<Unit> {
        if (keepTransaction) {
            synchronized(context.transactionRefCount) {
                context.transactionRefCount++
            }
        }

        val f = CompletableFuture<Unit>()

        thread {
            try {
                func()
            } catch (e: Exception) {
                context.transaction!!.rollback(false).get()

                if (onException != null) {
                    onException(e)
                }

                f.completeExceptionally(e)
            }

            var ended = false

            if (keepTransaction) {
                synchronized(context.transactionRefCount) {
                    context.transactionRefCount--

                    if (context.transactionRefCount <= 0) {
                        endTransaction()
                                .thenAccept { f.complete(Unit) }
                                .exceptionally {
                                    f.completeExceptionally(it)
                                    null
                                }

                        ended = true
                    }
                }
            }

            if (!ended) {
                f.complete(Unit)
            }
        }

        return f
    }

    protected fun runInBackground(keepTransaction: Boolean, func: () -> Unit) = runInBackground(keepTransaction, func, null)

    open fun init() {
        val c = this::class.java as Class<ManifoldScene<*>>

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

    open fun destroy() {
        if (eventNode != null) {
            ManifoldEventBus.unregister(eventNode!!)
            eventNodes.remove(this::class.java as Class<ManifoldScene<*>>)
        }
    }

    abstract fun stage(): CompletableFuture<R>

    fun execute(): CompletableFuture<R> {
        this@ManifoldScene.context.sessionIdentifier = sessionIdentifier

        val c = this@ManifoldScene::class.java as Class<ManifoldScene<*>>
        val interceptorClasses = Manifold.interceptors.getInterceptorsForScene(c)
        val functor = { stage() }

        val stack = Exception("Scene execution starts here")

        return future {
            if (interceptorClasses.isNotEmpty()) {
                val interceptorContext = SceneInterceptorContext(this@ManifoldScene)
                val interceptors = ArrayList<Pair<SceneInterceptorInfo, SceneInterceptor>>()

                try {
                    interceptorClasses.forEach {
                        val i = Manifold.dependencyProvider!!.get(it.interceptorClass)!!
                        interceptorContext.annotation = it.forAnnotation

                        interceptors.add(Pair(it, i))

                        i.m = m
                        i.before(interceptorContext).await()

                        if (interceptorContext.interceptorResult == InterceptorResult.Stop) {
                            throw InterceptorException("Interceptor ${i::class.java} stopped the execution of scene ${this@ManifoldScene::class.java}", interceptorContext.exception)
                        }
                    }

                    interceptorContext.result = functor().await()

                    if (!checkSuccessConditions(interceptorContext.result as R)) {
                        throw SceneFailedException(interceptorContext.result, stack)
                    }

                    interceptors.forEach {
                        val (info, i) = it

                        i.after(interceptorContext).await()

                        i.destroy(interceptorContext).await()
                    }

                    afterExecution().await()
                } catch (e: Throwable) {
                    interceptors.forEach {
                        val (info, i) = it

                        i.destroy(interceptorContext).await()
                    }

                    afterExecution(false).await()

                    val (shouldUseAlternativeResult, alternativeResult) = onFailure(e).await()

                    if (!shouldUseAlternativeResult) {
                        if (e is SceneFailedException) {
                            interceptorContext.result = e.data
                        } else {
                            throw e
                        }
                    } else {
                        interceptorContext.result = alternativeResult
                    }
                }

                return@future interceptorContext.result as R
            }

            var r: R

            try {
                r = functor().await()

                if (!checkSuccessConditions(r)) {
                    throw SceneFailedException(r, stack)
                }

                afterExecution().await()
            } catch (e: Throwable) {
                afterExecution(false).await()

                val (shouldUseAlternativeResult, alternativeResult) = onFailure(e).await()

                if (!shouldUseAlternativeResult) {
                    if (e is SceneFailedException) {
                        r = e.data as R
                    } else {
                        throw e
                    }
                } else {
                    r = alternativeResult as R
                }
            }

            return@future r
        }
    }

    private fun endTransaction(commit: Boolean = true) = future {
        if (this@ManifoldScene.context.transaction != null) {
            if (!this@ManifoldScene.context.autoCommit) {
                if (commit) {
                    this@ManifoldScene.context.transaction!!.commit().await()
                }
            }

            this@ManifoldScene.context.transaction!!.end().await()
        }
    }

    protected fun rollbackTransaction() = future {
        if (this@ManifoldScene.context.transaction != null) {
            if (!this@ManifoldScene.context.autoCommit) {
                this@ManifoldScene.context.transaction!!.rollback(false).await()
            }
        }
    }

    private fun afterExecution(commit: Boolean = true) = future {
        if (this@ManifoldScene.context.transactionRefCount <= 0) {
            endTransaction(commit).await()
        }
    }

    fun successOn(cond: (R) -> Boolean) {
        succCond1 = cond
    }

    fun successOn(cond: () -> R) {
        succCond2 = cond
    }

    fun successOn(expected: R) {
        succCond2 = { expected }
    }

    private fun checkSuccessConditions(r: R): Boolean {
        if (succCond1 != null) {
            return succCond1!!(r)
        } else if (succCond2 != null) {
            return succCond2!!() == r
        }

        return true
    }

    protected open fun onFailure(exception: Throwable): CompletableFuture<Pair<Boolean, R?>> {
        return CompletableFuture.completedFuture(Pair(false, null))
    }

    protected fun <T> hook(name: String, domain: String? = null, inputValue: T?): CompletableFuture<T?> {
        return Manifold.hooks.runHooks(name, domain, inputValue)
    }

    protected fun <T, H: Hook<T>> hook(h: Class<H>, domain: String? = null, inputValue: T?): CompletableFuture<T?> {
        return Manifold.hooks.runHooks(h, domain, inputValue)
    }
}