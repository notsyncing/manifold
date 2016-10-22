package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.*
import io.github.notsyncing.manifold.action.session.TimedVar
import io.github.notsyncing.manifold.storage.ManifoldStorage
import io.github.notsyncing.manifold.utils.DependencyProviderUtils
import kotlinx.coroutines.async
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

    fun execute() = async<R> {
        val c = this@ManifoldAction.javaClass as Class<ManifoldAction<*>>

        if (!Manifold.actionInterceptorMap.containsKey(c)) {
            val addToList = { info: ActionInterceptorInfo ->
                var list = Manifold.actionInterceptorMap[c]

                if (list == null) {
                    list = ArrayList()
                    Manifold.actionInterceptorMap[c] = list
                }

                list.add(info)
            }

            Manifold.actionInterceptors.forEach {
                val forActions = it.getAnnotation(ForActions::class.java)

                if (forActions != null) {
                    for (cl in forActions.value) {
                        if (cl.java == c) {
                            addToList(ActionInterceptorInfo(it, null))
                            break
                        }
                    }
                }

                val forAnnos = it.getAnnotation(ForActionsAnnotatedWith::class.java)

                if (forAnnos != null) {
                    for (cl in forAnnos.value) {
                        val a = c.getAnnotation(cl.java as Class<Annotation>)

                        if (a != null) {
                            addToList(ActionInterceptorInfo(it, a))
                        }
                    }
                }
            }

            if (!Manifold.actionInterceptorMap.containsKey(c)) {
                Manifold.actionInterceptorMap.put(c, ArrayList())
            }
        }

        val interceptorClasses = Manifold.actionInterceptorMap[c]!!
        val functor = { execute<ManifoldAction<R>> { this@ManifoldAction.action() } }

        if (interceptorClasses.size > 0) {
            val context = ActionInterceptorContext(this@ManifoldAction)
            val interceptors = interceptorClasses.map { Pair(it, Manifold.dependencyProvider!!.get(it.interceptorClass)) }

            interceptors.forEach {
                val (info, i) = it

                context.annotation = info.forAnnotation
                await(i!!.before(context))

                if (context.interceptorResult == InterceptorResult.Stop) {
                    throw InterceptorException("Interceptor ${i.javaClass} stopped the execution of action ${this@ManifoldAction.javaClass}", context.exception)
                }
            }

            context.result = await(functor())

            interceptors.forEach {
                val (info, i) = it

                context.annotation = info.forAnnotation
                await(i!!.after(context))
            }

            return@async context.result as R
        }

        return@async await(functor())
    }
}