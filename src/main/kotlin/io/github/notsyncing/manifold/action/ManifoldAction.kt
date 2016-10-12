package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptorContext
import io.github.notsyncing.manifold.action.interceptors.InterceptorResult
import io.github.notsyncing.manifold.action.session.TimedVar
import io.github.notsyncing.manifold.di.AutoProvide
import kotlinx.coroutines.async
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.javaField
import kotlin.reflect.memberProperties

abstract class ManifoldAction<R> {
    companion object {
        private val actionAutoProvidePropertyCache = ConcurrentHashMap<Class<ManifoldAction<*>>, ArrayList<KMutableProperty1<Any, Any?>>>()

        fun reset() {
            actionAutoProvidePropertyCache.clear()
        }
    }

    var sessionIdentifier: String? = null

    init {
        val c = this.javaClass as Class<ManifoldAction<*>>
        val propList: ArrayList<KMutableProperty1<Any, Any?>>

        if (!actionAutoProvidePropertyCache.containsKey(c)) {
            propList = ArrayList()
            actionAutoProvidePropertyCache[c] = propList

            this.javaClass.kotlin.memberProperties.filter { it.annotations.any { it.annotationClass == AutoProvide::class } }
                    .map { it as KMutableProperty1<Any, Any?>  }
                    .forEach { propList.add(it) }
        } else {
            propList = actionAutoProvidePropertyCache[c]!!
        }

        propList.forEach { it.set(this, Manifold.dependencyProvider?.get(it.javaField!!.type)) }
    }

    abstract protected fun action(): CompletableFuture<R>

    open fun withTransaction(trans: ManifoldTransaction<*>?): ManifoldAction<R> {
        return this
    }

    fun withSession(sid: String?): ManifoldAction<R> {
        sessionIdentifier = sid
        return this
    }

    fun <T> putTimedSessionVariable(key: String, value: TimedVar<T>) {
        if (sessionIdentifier == null) {
            return
        }

        Manifold.sessionStorageProvider?.put<T>(sessionIdentifier!!, key, value)
    }

    open protected fun <A: ManifoldAction<R>> execute(f: (A) -> CompletableFuture<R>) = f(this@ManifoldAction as A)

    fun execute() = async<R> {
        val interceptorClasses = Manifold.actionInterceptors[this@ManifoldAction.javaClass as Class<ManifoldAction<*>>]
        val functor = { execute<ManifoldAction<R>> { this@ManifoldAction.action() } }

        if (interceptorClasses != null) {
            val context = ActionInterceptorContext(this@ManifoldAction)
            val interceptors = interceptorClasses.map { it.newInstance() }

            interceptors.forEach {
                await(it.before(context))

                if (context.interceptorResult == InterceptorResult.Stop) {
                    throw InterruptedException("Interceptor ${it.javaClass} stopped the execution of action ${this@ManifoldAction.javaClass}")
                }
            }

            context.result = await(functor())

            interceptors.forEach {
                await(it.after(context))
            }

            return@async context.result as R
        }

        return@async await(functor())
    }
}