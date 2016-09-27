package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.session.TimedVar
import io.github.notsyncing.manifold.di.AutoProvide
import kotlinx.coroutines.async
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.javaField
import kotlin.reflect.memberProperties

abstract class ManifoldAction<T, R>(private var useTrans: Boolean = true,
                                    private var autoCommit: Boolean = true,
                                    private var transClass: Class<T>) {
    companion object {
        val actionAutoProvidePropertyCache = ConcurrentHashMap<Class<ManifoldAction<*, *>>, ArrayList<KMutableProperty1<Any, Any?>>>()

        fun reset() {
            actionAutoProvidePropertyCache.clear()
        }
    }

    var transaction: ManifoldTransaction<T>? = null
    var sessionIdentifier: String? = null

    init {
        val c = this.javaClass as Class<ManifoldAction<*, *>>
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

    abstract fun action(): CompletableFuture<R>

    fun withTransaction(trans: ManifoldTransaction<*>?): ManifoldAction<T, R> {
        transaction = trans as ManifoldTransaction<T>?
        return this
    }

    fun withSession(sid: String?): ManifoldAction<T, R> {
        sessionIdentifier = sid
        return this
    }

    fun <T> putTimedSessionVariable(key: String, value: TimedVar<T>) {
        if (sessionIdentifier == null) {
            return
        }

        Manifold.sessionStorageProvider?.put<T>(sessionIdentifier!!, key, value)
    }

    fun <A: ManifoldAction<*, R>> execute(f: (A) -> CompletableFuture<R>) = async<R> {
        if ((useTrans) && (Manifold.transactionProvider == null) && (transaction == null)) {
            throw RuntimeException("Action ${this@ManifoldAction.javaClass} wants to use transaction, but no transaction provider, nor a transaction is provided!")
        }

        var inTrans = false

        if (useTrans) {
            if (transaction == null) {
                transaction = Manifold.transactionProvider!!.get(transClass)
            } else {
                inTrans = true
            }

            if (useTrans) {
                await(transaction!!.begin(!autoCommit))
            }
        }

        try {
            val r = await(f(this@ManifoldAction as A))

            if (useTrans) {
                if ((!autoCommit) && (!inTrans)) {
                    await(transaction!!.commit())
                }

                if (!inTrans) {
                    await(transaction!!.end())
                }
            }

            return@async r
        } catch (e: Exception) {
            if ((useTrans) && (!inTrans)) {
                await(transaction!!.end())
            }

            throw e
        }
    }

    fun execute(): CompletableFuture<R> {
        return execute<ManifoldAction<T, R>> { this.action() }
    }
}