package io.github.notsyncing.manifold.di

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import java.lang.reflect.Constructor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ManifoldDependencyInjector : ManifoldDependencyProvider {
    companion object {
        private val singletons = ConcurrentHashMap<Class<*>, Any>()
        private val configs = ConcurrentHashMap<Class<*>, ProvideConfig>()
        private val classMap = ConcurrentHashMap<Class<*>, Class<*>>()
        private val constructorMap = ConcurrentHashMap<Class<*>, Constructor<*>>()
        var scanner = createScanner()

        private fun createScanner() = FastClasspathScanner("-com.github.mauricio", "-scala")
    }

    override fun init() {
        scanner.matchClassesWithAnnotation(EarlyProvide::class.java) { earlyProvide(it) }.scan()
    }

    override fun <T> get(type: Class<T>, singleton: Boolean): T {
        if (singletons.containsKey(type)) {
            return singletons[type] as T
        }

        val config = configs[type]

        var t: Class<*> = type

        if (classMap.containsKey(t)) {
            t = classMap[t]!!
        }

        var constructor: Constructor<*>? = constructorMap[t]

        if (constructor == null) {
            if (type.constructors.size > 1) {
                constructor = t.constructors.firstOrNull { it.isAnnotationPresent(AutoProvide::class.java) }
            } else {
                constructor = t.constructors.first()
            }

            if (constructor == null) {
                throw InstantiationException("Type $type has no annotated constructor for dependency injection, " +
                        "please mark the correct constructor with @${AutoProvide::class.java.simpleName}!")
            } else {
                constructorMap.put(t, constructor)
            }

            constructor.isAccessible = true
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

            o = constructor.newInstance(*params.toArray()) as T
        }

        if ((type.isAnnotationPresent(ProvideAsSingleton::class.java)) || (singleton)
                || (config?.singleton == true)) {
            singletons.put(type, o as Any)
        }

        return o
    }

    override fun reset() {
        singletons.clear()
        configs.clear()
        classMap.clear()
        constructorMap.clear()
        scanner = createScanner()
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
        singletons.put(obj.javaClass, obj)
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
        scanner.matchClassesWithAnnotation(anno) { handler.invoke(it as Class<A>) }.scan()
    }

    override fun <S> getAllSubclasses(superClass: Class<S>, handler: (Class<S>) -> Unit) {
        scanner.matchSubclassesOf(superClass) { handler.invoke(it as Class<S>) }.scan()
    }

    override fun <S> getAllClassesImplemented(implInterface: Class<S>, handler: (Class<S>) -> Unit) {
        scanner.matchClassesImplementing(implInterface) { handler.invoke(it as Class<S>) }.scan()
    }
}