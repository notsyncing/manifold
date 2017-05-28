package io.github.notsyncing.manifold.di

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ManifoldDependencyInjector : ManifoldDependencyProvider {
    companion object {
        private val singletons = ConcurrentHashMap<Class<*>, Any>()
        private val configs = ConcurrentHashMap<Class<*>, ProvideConfig>()
        private val classMap = ConcurrentHashMap<Class<*>, Class<*>>()
        private val constructorMap = ConcurrentHashMap<Class<*>, Constructor<*>>()
        var scanner = createScanner()
        var classScanResult: ScanResult = scanner.scan()

        private fun createScanner() = FastClasspathScanner("-com.github.mauricio", "-scala")

        var keepQuietOnResolveFailure = true
    }

    override fun init() {
        classScanResult.getNamesOfClassesWithAnnotation(EarlyProvide::class.java)
                .forEach { earlyProvide(Class.forName(it)) }
    }

    override fun <T> get(type: Class<T>, singleton: Boolean): T? {
        val config = configs[type]

        var t: Class<*> = type

        if (classMap.containsKey(t)) {
            t = classMap[t]!!
        }

        if (singletons.containsKey(t)) {
            return singletons[t] as T
        }

        var constructor: Constructor<*>? = constructorMap[t]

        if (constructor == null) {
            if (t.constructors.size > 1) {
                constructor = t.constructors.firstOrNull { it.isAnnotationPresent(AutoProvide::class.java) }
            } else if (t.constructors.size == 1) {
                constructor = t.constructors.first()
            } else {
                if (!keepQuietOnResolveFailure) {
                    throw InstantiationException("Type $type has no constructor, maybe you forgot to provide an implementation?")
                } else {
                    return null
                }
            }

            if (constructor == null) {
                throw InstantiationException("Type $type has no annotated constructor for dependency injection, " +
                        "please mark the correct constructor with @${AutoProvide::class.java.simpleName}!")
            } else {
                constructorMap.put(t, constructor)
            }
        }

        val o: T

        if (constructor.parameterCount <= 0) {
            o = t.newInstance() as T
        } else {
            val params = ArrayList<Any?>()

            constructor.parameters.forEach {
                if (it.isAnnotationPresent(NoProvide::class.java)) {
                    params.add(null)
                    return@forEach
                }

                val p = get(it.type)
                params.add(p)
            }

            try {
                o = constructor.newInstance(*params.toArray()) as T
            } catch (e: InvocationTargetException) {
                if (e.cause !is NullPointerException) {
                    throw e
                }

                if (!params.contains(null)) {
                    throw e
                }

                if (!keepQuietOnResolveFailure) {
                    throw e
                } else {
                    return null
                }
            }
        }

        if ((t.isAnnotationPresent(ProvideAsSingleton::class.java)) || (singleton)
                || (config?.singleton == true)) {
            singletons.put(t, o as Any)
        }

        return o
    }

    override fun reset() {
        singletons.clear()
        configs.clear()
        classMap.clear()
        constructorMap.clear()
        scanner = createScanner()
        classScanResult = scanner.scan()
    }

    private fun earlyProvide(c: Class<*>) {
        if (!c.isAnnotationPresent(ProvideAsSingleton::class.java)) {
            throw InstantiationException("Type $c must be singleton to be early provided!")
        }

        get(c)
    }

    fun has(type: Class<*>) = singletons.containsKey(type)

    override fun <T: S, S> registerAs(obj: T, type: Class<S>) {
        singletons.put(type, obj as Any)
    }

    override fun register(obj: Any) {
        singletons.put(obj::class.java, obj)
    }

    override fun <T: S, S> registerMapping(instClass: Class<T>, intfClass: Class<S>) {
        classMap.put(intfClass, instClass)
    }

    override fun registerSingleton(type: Class<*>) {
        val config = ProvideConfig()
        config.singleton = true

        configs.put(type, config)
    }

    override fun <A: Annotation> getAllAnnotated(anno: Class<A>, handler: (Class<*>) -> Unit) {
        classScanResult.getNamesOfClassesWithAnnotation(anno)
                ?.forEach { handler.invoke(Class.forName(it)) }
    }

    override fun <S> getAllSubclasses(superClass: Class<S>, handler: (Class<S>) -> Unit) {
        classScanResult.getNamesOfSubclassesOf(superClass)
                ?.forEach { handler.invoke(Class.forName(it) as Class<S>) }
    }

    override fun <S> getAllClassesImplemented(implInterface: Class<S>, handler: (Class<S>) -> Unit) {
        classScanResult.getNamesOfClassesImplementing(implInterface)
                ?.forEach { handler.invoke(Class.forName(it) as Class<S>) }
    }
}