package io.github.notsyncing.manifold

import com.alibaba.fastjson.JSON
import io.github.notsyncing.manifold.action.*
import io.github.notsyncing.manifold.action.interceptors.*
import io.github.notsyncing.manifold.action.session.ManifoldSessionStorage
import io.github.notsyncing.manifold.action.session.ManifoldSessionStorageProvider
import io.github.notsyncing.manifold.di.ManifoldDependencyInjector
import io.github.notsyncing.manifold.eventbus.EventBusNetWorker
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.event.InternalEvent
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.io.InvalidClassException
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object Manifold {
    var dependencyProvider: ManifoldDependencyProvider? = null
    var transactionProvider: ManifoldTransactionProvider? = null
    var sessionStorageProvider: ManifoldSessionStorageProvider? = null

    private val sceneTransitionConstructorCache = ConcurrentHashMap<Class<ManifoldScene<*>>, Constructor<ManifoldScene<*>>>()
    private val sceneEventConstructorCache = ConcurrentHashMap<Class<ManifoldScene<*>>, Constructor<ManifoldScene<*>>>()

    val sceneInterceptors = ConcurrentHashMap<Class<ManifoldScene<*>>, ArrayList<Class<SceneInterceptor>>>()
    val actionInterceptors = ConcurrentHashMap<Class<ManifoldAction<*>>, ArrayList<Class<ActionInterceptor>>>()

    fun init() {
        if (dependencyProvider == null) {
            dependencyProvider = ManifoldDependencyInjector()
        }

        if (sessionStorageProvider == null) {
            sessionStorageProvider = ManifoldSessionStorage()
        }

        ManifoldEventBus.init()

        processScenes()

        processInterceptors()
    }

    private fun processScenes() {
        dependencyProvider?.getAllSubclasses(ManifoldScene::class.java) {
            if (!Modifier.isAbstract(it.modifiers)) {
                try {
                    it.newInstance().init()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun processInterceptors() {
        dependencyProvider?.getAllClassesImplemented(Interceptor::class.java) { c ->
            if (Modifier.isAbstract(c.modifiers)) {
                return@getAllClassesImplemented
            }

            if (SceneInterceptor::class.java.isAssignableFrom(c)) {
                val a = c.getAnnotation(ForScenes::class.java)

                for (cl in a.value) {
                    val jc = cl.java as Class<ManifoldScene<*>>
                    var list = sceneInterceptors[jc]

                    if (list == null) {
                        list = ArrayList()
                        sceneInterceptors[jc] = list
                    }

                    list.add(c as Class<SceneInterceptor>)
                }
            } else if (ActionInterceptor::class.java.isAssignableFrom(c)) {
                val a = c.getAnnotation(ForActions::class.java)

                for (cl in a.value) {
                    val jc = cl.java as Class<ManifoldAction<*>>
                    var list2 = actionInterceptors[jc]

                    if (list2 == null) {
                        list2 = ArrayList()
                        actionInterceptors[jc] = list2
                    }

                    list2.add(c as Class<ActionInterceptor>)
                }
            } else {
                throw InvalidClassException(c.canonicalName, "${c.canonicalName} is not an interceptor class")
            }
        }
    }

    fun destroy(): CompletableFuture<Void> {
        reset()

        return ManifoldEventBus.stop().thenCompose {
            EventBusNetWorker.close()
        }
    }

    fun reset() {
        dependencyProvider = null
        sessionStorageProvider = null

        sceneTransitionConstructorCache.clear()
        sceneEventConstructorCache.clear()

        sceneInterceptors.clear()
        actionInterceptors.clear()

        ManifoldScene.reset()

        ManifoldAction.reset()
    }

    fun <R> run(action: ManifoldAction<R>, trans: ManifoldTransaction<*>? = null): CompletableFuture<R> {
        try {
            return action.withTransaction(trans).execute()
        } catch (e: Exception) {
            val c = CompletableFuture<R>()
            c.completeExceptionally(e)

            return c
        }
    }

    fun <R> run(sessionIdentifier: String? = null, f: (ManifoldActionContextRunner) -> CompletableFuture<R>): CompletableFuture<R> {
        val t = transactionProvider?.get()
        val runner = ManifoldActionContextRunner(sessionIdentifier, t)

        return f(runner)
    }

    fun <R> run(scene: ManifoldScene<R>, sessionIdentifier: String? = null): CompletableFuture<R> {
        return run(sessionIdentifier) { m ->
            scene.m = m
            scene.execute()
        }
    }

    fun <R> run(scene: Class<ManifoldScene<R>>, event: ManifoldEvent, sessionIdentifier: String? = null): CompletableFuture<R> {
        var constructor: Constructor<ManifoldScene<*>>?
        val params: Array<Any?>

        if (event.event == InternalEvent.TransitionToScene) {
            params = JSON.parseArray(event.data).toArray()
            constructor = sceneTransitionConstructorCache[scene as Class<ManifoldScene<*>>]

            if (constructor == null) {
                constructor = scene.constructors.firstOrNull { it.isAnnotationPresent(ForTransition::class.java) } as Constructor<ManifoldScene<*>>?

                if (constructor == null) {
                    throw NoSuchMethodException("No constructor of scene $scene has parameter count of ${params.count()}")
                }

                sceneTransitionConstructorCache[scene as Class<ManifoldScene<*>>] = constructor
            }
        } else {
            params = arrayOf(event)
            constructor = sceneEventConstructorCache[scene as Class<ManifoldScene<*>>]

            if (constructor == null) {
                try {
                    constructor = scene.getConstructor(ManifoldEvent::class.java) as Constructor<ManifoldScene<*>>
                } catch (e: NoSuchMethodException) {
                    throw NoSuchMethodException("No constructor of scene $scene has only one event parameter!")
                }

                sceneEventConstructorCache[scene as Class<ManifoldScene<*>>] = constructor
            }
        }

        val s = constructor.newInstance(*params)
        return Manifold.run(s, sessionIdentifier) as CompletableFuture<R>
    }
}