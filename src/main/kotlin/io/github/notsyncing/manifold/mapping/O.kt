package io.github.notsyncing.manifold.mapping

import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

object O {
    private fun _shallowCopyFields(from: Any, to: Any, skipFields: Set<String>?) {
        val fromFields = from.javaClass.kotlin.memberProperties
                .filter { it is KMutableProperty<*> }
                .filter { !(skipFields?.contains(it.name) ?: false) }
                .map { it as KMutableProperty<*> }
                .groupingBy { it.name }
                .aggregate { _, _: KMutableProperty<*>?, element, _ -> element }

        val toFields = to.javaClass.kotlin.memberProperties
                .filter { it is KMutableProperty<*> }
                .map { it as KMutableProperty<*> }
                .groupingBy { it.name }
                .aggregate { _, _: KMutableProperty<*>?, element, _ -> element }

        if (fromFields.size > toFields.size) {
            for ((name, f) in toFields) {
                if (!fromFields.containsKey(name)) {
                    continue
                }

                f.setter.call(to, fromFields[name]!!.getter.call(from))
            }
        } else {
            for ((name, f) in fromFields) {
                if (!toFields.containsKey(name)) {
                    continue
                }

                toFields[name]!!.setter.call(to, f.getter.call(from))
            }
        }
    }

    private fun shallowCopyFields(from: Any, to: Any, skipFields: Set<KProperty<*>>?) {
        _shallowCopyFields(from, to, skipFields?.map { it.name }?.toSet())
    }

    fun <R> map(from: Any?, toType: Class<R>): R {
        if (from == null) {
            return null as R
        }

        val to = toType.newInstance()

        if (to == null) {
            throw InstantiationException("Failed to create new instance of $toType, please make sure it has a no-arg " +
                    "constructor!")
        }

        shallowCopyFields(from, to, null)

        return to
    }

    inline fun <reified R> map(from: Any?): R {
        return map(from, R::class.java)
    }

    fun <R> map(from: List<*>, toType: Class<R>): List<R> {
        return from.map { if (it == null) null else map(it, toType) } as List<R>
    }

    inline fun <reified R> map(from: List<*>): List<R> {
        return map(from, R::class.java)
    }

    fun <F, T> fill(from: F?, to: T, filler: (F, T) -> Unit) {
        if (from == null) {
            return
        }

        filler(from, to)
    }

    fun <F, T> fill(from: F?, to: T) {
        fill(from, to, null as Set<KProperty<*>>?)
    }

    fun <F, T> fill(from: F?, to: T, skipFields: Set<KProperty<*>>?) {
        fill(from, to) { f, t -> shallowCopyFields(f!!, t!!, skipFields) }
    }

    fun <F, T> fill(from: List<F>, to: List<T>, filler: (F, T) -> Unit) {
        for (i in 0 until from.size) {
            if (to[i] == null) {
                continue
            }

            fill(from[i], to[i]!!, filler)
        }
    }

    fun <F, T> fill(from: List<F>, to: List<T>) {
        fill(from, to) { f: F, t -> shallowCopyFields(f!!, t!!, null) }
    }

    fun <F, T> fill(from: List<F>, to: List<T>, skipFields: Set<KProperty<*>>?) {
        fill(from, to) { f: F, t -> shallowCopyFields(f!!, t!!, skipFields) }
    }
}

fun <R> Any?.mapAs(type: Class<R>): R {
    return O.map(this, type)
}

inline fun <reified R> Any?.mapAs(): R {
    return O.map(this)
}

fun <R> List<*>.mapAs(type: Class<R>): List<R> {
    return O.map(this, type)
}

inline fun <reified R> List<*>.mapAs(): List<R> {
    return O.map(this)
}

fun List<*>.fillFrom(from: List<*>) {
    O.fill(from, this)
}

fun List<*>.fillFrom(from: List<*>, skipFields: Set<KProperty<*>>?) {
    O.fill(from, this, skipFields)
}

fun <F, T> List<T>.fillFrom(from: List<F>, filler: (F, T) -> Unit) {
    O.fill(from, this, filler)
}

fun List<*>.fillTo(to: List<*>) {
    O.fill(this, to)
}

fun List<*>.fillTo(to: List<*>, skipFields: Set<KProperty<*>>?) {
    O.fill(this, to, skipFields)
}

fun <F, T> List<F>.fillTo(to: List<T>, filler: (F, T) -> Unit) {
    O.fill(this, to, filler)
}

fun Any.fillFrom(from: Any?) {
    O.fill(from, this)
}

fun Any.fillFrom(from: Any?, skipFields: Set<KProperty<*>>?) {
    O.fill(from, this, skipFields)
}

fun <F, T> T.fillFrom(from: F?, filler: (F, T) -> Unit) {
    O.fill(from, this, filler)
}

fun Any?.fillTo(to: Any) {
    O.fill(this, to)
}

fun Any?.fillTo(to: Any, skipFields: Set<KProperty<*>>?) {
    O.fill(this, to, skipFields)
}

fun <F, T> F.fillTo(to: T, filler: (F, T) -> Unit) {
    O.fill(this, to, filler)
}