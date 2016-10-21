package io.github.notsyncing.manifold.action

import com.alibaba.fastjson.JSON
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.*
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.InternalEvent
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.utils.DependencyProviderUtils
import kotlinx.coroutines.async
import java.util.*
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

    open fun init() {

    }

    abstract protected fun stage(): CompletableFuture<R>

    fun execute() = async<R> {
        context.sessionIdentifier = sessionIdentifier

        val c = this@ManifoldScene.javaClass as Class<ManifoldScene<*>>

        if (!Manifold.sceneInterceptorMap.containsKey(c)) {
            val addToList = { info: SceneInterceptorInfo ->
                var list = Manifold.sceneInterceptorMap[c]

                if (list == null) {
                    list = ArrayList()
                    Manifold.sceneInterceptorMap[c] = list
                }

                list.add(info)
            }

            Manifold.sceneInterceptors.forEach {
                val forScenes = it.getAnnotation(ForScenes::class.java)

                if (forScenes != null) {
                    for (cl in forScenes.value) {
                        if (cl.java == c) {
                            addToList(SceneInterceptorInfo(it, null))
                            break
                        }
                    }
                }

                val forAnnos = it.getAnnotation(ForScenesAnnotatedWith::class.java)

                if (forAnnos != null) {
                    for (cl in forAnnos.value) {
                        val a = c.getAnnotation(cl.java as Class<Annotation>)

                        if (a != null) {
                            addToList(SceneInterceptorInfo(it, a))
                        }
                    }
                }
            }

            if (!Manifold.sceneInterceptorMap.containsKey(c)) {
                Manifold.sceneInterceptorMap.put(c, ArrayList())
            }
        }

        val interceptorClasses = Manifold.sceneInterceptorMap[c]!!
        val functor = { stage() }

        if (interceptorClasses.size > 0) {
            val interceptorContext = SceneInterceptorContext(this@ManifoldScene)
            val interceptors = interceptorClasses.map { Pair(it, Manifold.dependencyProvider!!.get(it.interceptorClass)) }

            interceptors.forEach {
                val (info, i) = it

                interceptorContext.annotation = info.forAnnotation

                i!!.m = m
                i.before(interceptorContext)

                if (interceptorContext.interceptorResult == InterceptorResult.Stop) {
                    throw InterruptedException("Interceptor ${it.javaClass} stopped the execution of scene ${this@ManifoldScene.javaClass}")
                }
            }

            interceptorContext.result = await(functor())

            interceptors.forEach {
                val (info, i) = it

                interceptorContext.annotation = info.forAnnotation
                i!!.after(interceptorContext)
            }

            await(afterExecution())

            return@async interceptorContext.result as R
        }

        val r = await(functor())

        await(afterExecution())

        return@async r
    }

    private fun afterExecution() = async<Unit> {
        if (context.transaction != null) {
            if (!context.autoCommit) {
                await(context.transaction!!.commit())
                await(context.transaction!!.end())
            }
        }
    }
}