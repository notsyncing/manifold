package io.github.notsyncing.manifold

import com.alibaba.fastjson.JSON
import io.github.notsyncing.manifold.action.*
import io.github.notsyncing.manifold.action.interceptors.*
import io.github.notsyncing.manifold.action.session.ManifoldSessionStorage
import io.github.notsyncing.manifold.action.session.ManifoldSessionStorageProvider
import io.github.notsyncing.manifold.authenticate.AuthenticateInformationProvider
import io.github.notsyncing.manifold.di.ManifoldDependencyInjector
import io.github.notsyncing.manifold.eventbus.EventBusNetWorker
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.event.InternalEvent
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.manifold.feature.FeaturePublisher
import io.github.notsyncing.manifold.utils.DependencyProviderUtils
import io.vertx.core.impl.ConcurrentHashSet
import java.io.InvalidClassException
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object Manifold {
    var dependencyProvider: ManifoldDependencyProvider? = null
    var transactionProvider: ManifoldTransactionProvider? = null
    var sessionStorageProvider: ManifoldSessionStorageProvider? = null
    var authInfoProvider: AuthenticateInformationProvider? = null

    private val sceneTransitionConstructorCache = ConcurrentHashMap<Class<ManifoldScene<*>>, Constructor<ManifoldScene<*>>>()
    private val sceneEventConstructorCache = ConcurrentHashMap<Class<ManifoldScene<*>>, Constructor<ManifoldScene<*>>>()

    val sceneInterceptors = ArrayList<Class<SceneInterceptor>>()
    val sceneInterceptorMap = ConcurrentHashMap<Class<ManifoldScene<*>>, ArrayList<SceneInterceptorInfo>>()
    val actionInterceptors = ArrayList<Class<ActionInterceptor>>()
    val actionInterceptorMap = ConcurrentHashMap<Class<ManifoldAction<*>>, ArrayList<ActionInterceptorInfo>>()

    val sceneBgWorkerPool = Executors.newFixedThreadPool(100)

    var enableFeatureManagement = true

    val enabledFeatures = ConcurrentHashSet<String>()
    val enabledFeatureGroups = ConcurrentHashSet<String>()
    val disabledFeatures = ConcurrentHashSet<String>()
    val disabledFeatureGroups = ConcurrentHashSet<String>()

    var featurePublisher: FeaturePublisher? = null

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

    inline fun <reified T: AuthenticateInformationProvider> authInfoProvider(): Manifold {
        authInfoProvider = dependencyProvider!!.get(T::class.java)
        return this
    }

    fun enableFeatures(vararg features: String) {
        features.forEach { enabledFeatures.add(it) }
    }

    fun disableFeatures(vararg features: String) {
        features.forEach {
            enabledFeatures.remove(it)
            disabledFeatures.add(it)
        }
    }

    fun enableFeatureGroups(vararg featureGroups: String) {
        featureGroups.forEach { enabledFeatureGroups.add(it) }
    }

    fun disableFeatureGroups(vararg featureGroups: String) {
        featureGroups.forEach {
            enabledFeatureGroups.remove(it)
            disabledFeatureGroups.add(it)
        }
    }

    fun <T: ManifoldScene<*>> isFeatureEnabled(sceneClass: Class<T>): Boolean {
        if (!enableFeatureManagement) {
            return true
        }

        val featureAnno = sceneClass.getAnnotation(Feature::class.java)

        if (featureAnno == null) {
            return false
        }

        if (disabledFeatures.contains(featureAnno.value)) {
            return false
        }

        if (featureAnno.groups.any { disabledFeatureGroups.contains(it) }) {
            return false
        }

        if (enabledFeatures.contains(featureAnno.value)) {
            return true
        }

        if (featureAnno.groups.any { enabledFeatureGroups.contains(it) }) {
            return true
        }

        return false
    }

    private fun processScenes() {
        dependencyProvider?.getAllSubclasses(ManifoldScene::class.java) {
            if (Modifier.isAbstract(it.modifiers)) {
                return@getAllSubclasses
            }

            if (!isFeatureEnabled(it)) {
                return@getAllSubclasses
            }

            try {
                it.newInstance().init()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (featurePublisher != null) {
                featurePublisher!!.publishFeature(it)
            }
        }
    }

    private fun processInterceptors() {
        dependencyProvider?.getAllSubclasses(Interceptor::class.java) { c ->
            if (Modifier.isAbstract(c.modifiers)) {
                return@getAllSubclasses
            }

            if (c.annotations.isEmpty()) {
                return@getAllSubclasses
            }

            if (SceneInterceptor::class.java.isAssignableFrom(c)) {
                sceneInterceptors.add(c as Class<SceneInterceptor>)
            } else if (ActionInterceptor::class.java.isAssignableFrom(c)) {
                actionInterceptors.add(c as Class<ActionInterceptor>)
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
        sceneInterceptorMap.clear()
        actionInterceptors.clear()
        actionInterceptorMap.clear()

        enabledFeatures.clear()
        enabledFeatureGroups.clear()
        disabledFeatures.clear()
        disabledFeatureGroups.clear()

        ManifoldScene.reset()

        DependencyProviderUtils.reset()
    }

    fun <R> run(action: ManifoldAction<R>): CompletableFuture<R> {
        try {
            return action.execute()
        } catch (e: Exception) {
            val c = CompletableFuture<R>()
            c.completeExceptionally(e)

            return c
        }
    }

    fun <R> run(sessionIdentifier: String? = null, sceneContext: SceneContext? = null, f: (ManifoldActionContextRunner) -> CompletableFuture<R>): CompletableFuture<R> {
        val runner = ManifoldActionContextRunner(sessionIdentifier, sceneContext)

        return f(runner)
    }

    fun <R> run(scene: ManifoldScene<R>, sessionIdentifier: String? = null): CompletableFuture<R> {
        return run(sessionIdentifier, scene.context) { m ->
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

                sceneTransitionConstructorCache[scene] = constructor
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

                sceneEventConstructorCache[scene] = constructor
            }
        }

        val s = constructor.newInstance(*params)
        return Manifold.run(s, sessionIdentifier) as CompletableFuture<R>
    }
}