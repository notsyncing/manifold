package io.github.notsyncing.manifold.di

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.notsyncing.manifold.ManifoldDependencyProvider
import java.lang.reflect.Constructor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ManifoldDependencyInjector : ManifoldDependencyProvider {
    companion object {
        private val singletons = ConcurrentHashMap<Class<*>, Any>()
        var scanner = FastClasspathScanner()
    }

    init {
        scanner.matchClassesWithAnnotation(EarlyProvide::class.java) { earlyProvide(it) }.scan()
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
        scanner = FastClasspathScanner()
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

    fun <A: Annotation> getAllAnnotated(anno: Class<A>, handler: (Class<*>) -> Unit) {
        scanner.matchClassesWithAnnotation(anno) { handler.invoke(it as Class<A>) }.scan()
    }

    fun <A: Annotation> getAllAnnotated(anno: Class<A>): Array<Class<*>> {
        val list = ArrayList<Class<*>>()

        getAllAnnotated(anno) { list.add(it) }

        return list.toArray(arrayOf())
    }

    fun <S> getAllSubclasses(superClass: Class<S>, handler: (Class<S>) -> Unit) {
        scanner.matchSubclassesOf(superClass) { handler.invoke(it as Class<S>) }.scan()
    }

    fun <S> getAllSubclasses(superClass: Class<S>): Array<Class<S>> {
        val list = ArrayList<Class<S>>()

        getAllSubclasses(superClass) { list.add(it) }

        return list.toArray(arrayOf())
    }

    fun <S> getAllClassesImplemented(implInterface: Class<S>, handler: (Class<S>) -> Unit) {
        scanner.matchClassesImplementing(implInterface) { handler.invoke(it as Class<S>) }.scan()
    }

    fun <S> getAllClassesImplemented(implInterface: Class<S>): Array<Class<S>> {
        val list = ArrayList<Class<S>>()

        getAllClassesImplemented(implInterface) { list.add(it) }

        return list.toArray(arrayOf())
    }
}