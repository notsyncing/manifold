package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import io.github.notsyncing.manifold.action.interceptors.InterceptorException
import io.github.notsyncing.manifold.action.interceptors.InterceptorResult
import io.github.notsyncing.manifold.action.session.TimedVar
import io.github.notsyncing.manifold.hooking.Hook
import io.github.notsyncing.manifold.storage.ManifoldStorage
import io.github.notsyncing.manifold.utils.DependencyProviderUtils
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.*
import java.util.concurrent.CompletableFuture

abstract class ManifoldAction<R> {
    var sessionIdentifier: String? = null
    lateinit var context: SceneContext

    protected val storageList = ArrayList<ManifoldStorage<*>>()

    init {
        DependencyProviderUtils.autoProvideProperties(this, this::provideDependency)
    }

    open protected fun provideDependency(t: Class<*>): Any? {
        return Manifold.dependencyProvider?.get(t)
    }

    abstract protected fun action(): CompletableFuture<R>

    fun withSession(sid: String?): ManifoldAction<R> {
        sessionIdentifier = sid
        return this
    }

    fun withContext(ctx: SceneContext?): ManifoldAction<R> {
        if (ctx != null) {
            this.context = ctx
        }

        return this
    }

    fun <T> putTimedSessionVariable(key: String, value: TimedVar<T>) {
        if (sessionIdentifier == null) {
            return
        }

        Manifold.sessionStorageProvider?.put<T>(sessionIdentifier!!, key, value)
    }

    open protected fun <A: ManifoldAction<R>> execute(f: (A) -> CompletableFuture<R>) = f(this@ManifoldAction as A)

    fun execute(): CompletableFuture<R> {
        val c = this@ManifoldAction::class.java as Class<ManifoldAction<*>>
        val interceptorClasses = Manifold.interceptors.getInterceptorsForAction(c)
        val functor = { execute<ManifoldAction<R>> { this@ManifoldAction.action() } }

        return future {
            if (interceptorClasses.isNotEmpty()) {
                val context = ActionInterceptorContext(this@ManifoldAction)
                val interceptors = interceptorClasses.map { Pair(it, Manifold.dependencyProvider!!.get(it.interceptorClass)) }

                interceptors.forEach {
                    val (info, i) = it

                    context.annotation = info.forAnnotation
                    i!!.before(context).await()

                    if (context.interceptorResult == InterceptorResult.Stop) {
                        throw InterceptorException("Interceptor ${i::class.java} stopped the execution of action ${this@ManifoldAction::class.java}", context.exception)
                    }
                }

                context.result = functor().await()

                interceptors.forEach {
                    val (info, i) = it

                    context.annotation = info.forAnnotation
                    i!!.after(context).await()
                }

                return@future context.result as R
            }

            return@future functor().await()
        }
    }

    protected fun <T> hook(name: String, domain: String? = null, inputValue: T?): CompletableFuture<T?> {
        return Manifold.hooks.runHooks(name, domain, inputValue)
    }

    protected fun <T, H: Hook<T>> hook(h: Class<H>, domain: String? = null, inputValue: T?): CompletableFuture<T?> {
        return Manifold.hooks.runHooks(h, domain, inputValue)
    }
}