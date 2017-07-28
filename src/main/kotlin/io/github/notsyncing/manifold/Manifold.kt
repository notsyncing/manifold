package io.github.notsyncing.manifold

import com.alibaba.fastjson.JSON
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult
import io.github.notsyncing.manifold.action.*
import io.github.notsyncing.manifold.action.interceptors.ActionInterceptor
import io.github.notsyncing.manifold.action.interceptors.Interceptor
import io.github.notsyncing.manifold.action.interceptors.InterceptorManager
import io.github.notsyncing.manifold.action.interceptors.SceneInterceptor
import io.github.notsyncing.manifold.action.session.ManifoldSessionStorage
import io.github.notsyncing.manifold.action.session.ManifoldSessionStorageProvider
import io.github.notsyncing.manifold.authenticate.AuthenticateInformationProvider
import io.github.notsyncing.manifold.di.ManifoldDependencyInjector
import io.github.notsyncing.manifold.domain.ManifoldDomain
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.event.InternalEvent
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.feature.FeatureManager
import io.github.notsyncing.manifold.feature.FeaturePublisher
import io.github.notsyncing.manifold.utils.DependencyProviderUtils
import java.io.InvalidClassException
import java.io.InvalidObjectException
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object Manifold {
    var dependencyProvider: ManifoldDependencyProvider? = null
    var transactionProvider: ManifoldTransactionProvider? = null
    var sessionStorageProvider: ManifoldSessionStorageProvider? = null
    var authInfoProvider: AuthenticateInformationProvider?
        get() {
            if (ownAuthInfoProvider != null) {
                return ownAuthInfoProvider
            }

            if (authInfoProviderClass == null) {
                return null
            }

            return dependencyProvider?.get(authInfoProviderClass!!)
        }
        set(value) {
            ownAuthInfoProvider = value
        }

    var authInfoProviderClass: Class<AuthenticateInformationProvider>? = null
    private var ownAuthInfoProvider: AuthenticateInformationProvider? = null

    private val sceneTransitionConstructorCache = ConcurrentHashMap<Class<ManifoldScene<*>>, Constructor<ManifoldScene<*>>>()
    private val sceneEventConstructorCache = ConcurrentHashMap<Class<ManifoldScene<*>>, Constructor<ManifoldScene<*>>>()

    val actionMetadata = ConcurrentHashMap<String, Class<ManifoldAction<*>>>()
    val interceptors = InterceptorManager()

    val sceneBgWorkerPool = Executors.newFixedThreadPool(100)

    val features = FeatureManager()

    var enableFeatureManagement: Boolean
        get() = features.enableFeatureManagement
        set(value) {
            features.enableFeatureManagement = value
        }

    var featurePublisher: FeaturePublisher?
        get() = features.featurePublisher
        set(value) {
            features.featurePublisher = value

            features.republishAllFeatures()
        }

    private val onDestroyListeners = mutableListOf<() -> Unit>()
    private val onResetListeners = mutableListOf<() -> Unit>()
    private val onInitListeners = mutableListOf<() -> Unit>()

    val rootDomain = ManifoldDomain()

    fun init() {
        rootDomain.init()

        if (dependencyProvider == null) {
            dependencyProvider = ManifoldDependencyInjector(rootDomain)
        }

        if (sessionStorageProvider == null) {
            sessionStorageProvider = ManifoldSessionStorage()
        }

        ManifoldEventBus.init()

        processScenes()

        processActions()

        processInterceptors()

        onInitListeners.forEach { it() }

        ManifoldDomain.afterScan {
            processScenes(it)
            processActions(it)
            processInterceptors(it)
        }
    }

    private fun processScenes(domain: ManifoldDomain = rootDomain) {
        val handler = { s: ScanResult, cl: ClassLoader ->
            s.getNamesOfSubclassesOf(ManifoldScene::class.java).forEach {
                val clazz = Class.forName(it, true, cl)

                if (Modifier.isAbstract(clazz.modifiers)) {
                    return@forEach
                }

                features.registerFeature(clazz as Class<ManifoldScene<*>>)
            }

            features.publishFeatures()
        }

        if (domain == rootDomain) {
            domain.inAllClassScanResults(handler)
        } else {
            domain.inCurrentClassScanResult(handler)
        }
    }

    private fun processActions(domain: ManifoldDomain = rootDomain) {
        val handler = { s: ScanResult, cl: ClassLoader ->
            s.getNamesOfSubclassesOf(ManifoldAction::class.java).forEach {
                val clazz = Class.forName(it, true, cl)

                if (clazz.isAnnotationPresent(ActionMetadata::class.java)) {
                    val metadata = clazz.getAnnotation(ActionMetadata::class.java)

                    val actionName = if (domain.name == ManifoldDomain.ROOT)
                        metadata.value
                    else
                        domain.name + "_" + metadata.value

                    if (actionMetadata.contains(actionName)) {
                        throw InvalidObjectException("Action metadata $actionName of $it conflicted with " +
                                "previous ${actionMetadata[actionName]}")
                    }

                    actionMetadata[actionName] = clazz as Class<ManifoldAction<*>>
                }
            }
        }

        if (domain == rootDomain) {
            domain.inAllClassScanResults(handler)
        } else {
            domain.inCurrentClassScanResult(handler)
        }
    }

    private fun processInterceptors(domain: ManifoldDomain = rootDomain) {
        val handler = { s: ScanResult, cl: ClassLoader ->
            s.getNamesOfSubclassesOf(Interceptor::class.java).forEach {
                val clazz = Class.forName(it, true, cl)

                if (Modifier.isAbstract(clazz.modifiers)) {
                    return@forEach
                }

                if (clazz.annotations.isEmpty()) {
                    return@forEach
                }

                if (SceneInterceptor::class.java.isAssignableFrom(clazz)) {
                    interceptors.addSceneInterceptor(clazz as Class<SceneInterceptor>)
                } else if (ActionInterceptor::class.java.isAssignableFrom(clazz)) {
                    interceptors.addActionInterceptor(clazz as Class<ActionInterceptor>)
                } else {
                    throw InvalidClassException(clazz.canonicalName, "${clazz.canonicalName} is not an supported " +
                            "interceptor class")
                }
            }
        }

        if (domain == rootDomain) {
            domain.inAllClassScanResults(handler)
        } else {
            domain.inCurrentClassScanResult(handler)
        }
    }

    fun destroy(): CompletableFuture<Void> {
        reset()

        onDestroyListeners.forEach { it() }

        sceneBgWorkerPool.shutdown()

        rootDomain.close()

        return ManifoldEventBus.stop()
    }

    fun reset() {
        dependencyProvider = null

        if (sessionStorageProvider != null) {
            sessionStorageProvider!!.clear()
        }

        sessionStorageProvider = null

        authInfoProvider = null
        authInfoProviderClass = null
        ownAuthInfoProvider = null

        sceneTransitionConstructorCache.clear()
        sceneEventConstructorCache.clear()

        interceptors.reset()

        features.destroy()

        ManifoldScene.reset()

        DependencyProviderUtils.reset()

        rootDomain.reset()

        onResetListeners.forEach { it() }
    }

    inline fun <reified T: AuthenticateInformationProvider> authInfoProvider(): Manifold {
        authInfoProviderClass = T::class.java as Class<AuthenticateInformationProvider>
        return this
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
        if (!features.isFeatureEnabled(scene::class.java)) {
            throw IllegalAccessException("Feature scene $scene is not enabled!")
        }

        return run(sessionIdentifier, scene.context) { m ->
            scene.m = m
            scene.execute()
        }
    }

    fun <R> run(scene: Class<ManifoldScene<R>>, event: ManifoldEvent, sessionIdentifier: String? = null): CompletableFuture<R> {
        if (!features.isFeatureEnabled(scene)) {
            throw IllegalAccessException("Feature scene $scene is not enabled!")
        }

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
                    constructor = scene.getConstructor(ManifoldEvent::class.java)

                    if (constructor == null) {
                        throw NoSuchMethodException("In scene $scene")
                    }
                } catch (e: NoSuchMethodException) {
                    throw NoSuchMethodException("No constructor of scene $scene has only one event parameter!")
                }

                sceneEventConstructorCache[scene] = constructor
            }
        }

        val s = constructor.newInstance(*params)
        return Manifold.run(s, sessionIdentifier) as CompletableFuture<R>
    }

    fun addOnDestroyListener(listener: () -> Unit) {
        onDestroyListeners.add(listener)
    }

    fun addOnResetListener(listener: () -> Unit) {
        onResetListeners.add(listener)
    }

    fun addOnInitListener(listener: () -> Unit) {
        onInitListeners.add(listener)
    }
}