package io.github.notsyncing.manifold.di

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import java.lang.reflect.Constructor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ManifoldDependencyInjector : ManifoldDependencyProvider {
    companion object {
        private val singletons = ConcurrentHashMap<Class<*>, Any>()
        private var cpScanner = FastClasspathScanner()
    }

    init {
        cpScanner.matchClassesWithAnnotation(EarlyProvide::class.java) { earlyProvide(it) }.scan()
    }

    override fun <T> get(type: Class<T>): T {
        if (singletons.containsKey(type)) {
            return singletons[type] as T
        }

        val constructor: Constructor<*>?

        if (type.constructors.size > 1) {
            constructor = type.constructors.firstOrNull { it.isAnnotationPresent(AutoProvide::class.java) }
        } else {
            constructor = type.constructors.first()
        }

        if (constructor == null) {
            throw InstantiationException("Type $type has no annotated constructor for dependency injection!")
        }

        val o: T

        if (constructor.parameterCount <= 0) {
            o = type.newInstance()
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

        if (type.isAnnotationPresent(ProvideAsSingleton::class.java)) {
            singletons.put(type, o as Any)
        }

        return o
    }

    fun reset() {
        singletons.clear()
        cpScanner = FastClasspathScanner()
    }

    private fun earlyProvide(c: Class<*>) {
        if (!c.isAnnotationPresent(ProvideAsSingleton::class.java)) {
            throw InstantiationException("Type $c must be singleton to be early provided!")
        }

        get(c)
    }

    fun has(type: Class<*>) = singletons.containsKey(type)

    fun <T: S, S> registerAs(obj: T, type: Class<S>) {
        singletons.put(type, obj as Any)
    }

    fun register(obj: Any) {
        singletons.put(obj.javaClass, obj)
    }
}