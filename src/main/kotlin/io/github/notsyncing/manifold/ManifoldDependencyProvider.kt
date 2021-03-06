package io.github.notsyncing.manifold

import io.github.notsyncing.manifold.domain.ManifoldDomain
import java.nio.file.Path
import java.util.*

interface ManifoldDependencyProvider {
    fun reset()

    fun init()

    fun <T> get(type: Class<T>): T? {
        return get(type, false)
    }

    fun <T> get(type: Class<T>, singleton: Boolean = false, provides: Array<*> = emptyArray<Any>()): T?

    fun register(obj: Any)

    fun <T: S, S> registerAs(obj: T, type: Class<S>)

    fun <T: S, S> registerMapping(instClass: Class<T>, intfClass: Class<S>)

    fun <T> unregisterMapping(instClass: Class<T>)

    fun registerSingleton(type: Class<*>)

    fun <A: Annotation> getAllAnnotated(anno: Class<A>, handler: (Class<*>) -> Unit)

    fun <A: Annotation> getAllAnnotated(anno: Class<A>): Array<Class<*>> {
        val list = ArrayList<Class<*>>()

        getAllAnnotated(anno) { list.add(it) }

        return list.toArray(arrayOf())
    }

    fun <S> getAllSubclasses(superClass: Class<S>, handler: (Class<S>) -> Unit)

    fun <S> getAllSubclasses(superClass: Class<S>): Array<Class<S>> {
        val list = ArrayList<Class<S>>()

        getAllSubclasses(superClass) { list.add(it) }

        return list.toArray(arrayOf())
    }

    fun <S> getAllClassesImplemented(implInterface: Class<S>, handler: (Class<S>) -> Unit)

    fun <S> getAllClassesImplemented(implInterface: Class<S>): Array<Class<S>> {
        val list = ArrayList<Class<S>>()

        getAllClassesImplemented(implInterface) { list.add(it) }

        return list.toArray(arrayOf())
    }

    fun getAllClasspathFiles(handler: (Path, String) -> Unit) {
        throw UnsupportedOperationException("Not implemented")
    }

    fun getAllClasspathFiles(): List<Pair<Path, String>> {
        val list = mutableListOf<Pair<Path, String>>()

        getAllClasspathFiles { p, s -> list.add(Pair(p, s)) }

        return list
    }

    fun getAllClasspathFilesWithDomain(handler: (ManifoldDomain, Path, String) -> Unit) {
        throw UnsupportedOperationException("Not implemented")
    }

    fun getAllClasspathFilesWithDomain(): List<Triple<ManifoldDomain, Path, String>> {
        val list = mutableListOf<Triple<ManifoldDomain, Path, String>>()

        getAllClasspathFilesWithDomain { d, p, s -> list.add(Triple(d, p, s)) }

        return list
    }
}