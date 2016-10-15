package io.github.notsyncing.manifold.utils

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.di.AutoProvide
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.javaField
import kotlin.reflect.memberProperties

object DependencyProviderUtils {
    private val propertyCache = ConcurrentHashMap<Class<*>, ArrayList<KMutableProperty1<Any, Any?>>>()

    fun reset() {
        propertyCache.clear()
    }

    fun autoProvideProperties(c: Class<*>, provideDependency: (Class<*>) -> Any? = { Manifold.dependencyProvider?.get(it) }) {
        val propList: ArrayList<KMutableProperty1<Any, Any?>>

        if (!propertyCache.containsKey(c)) {
            propList = ArrayList()
            propertyCache[c] = propList

            c.kotlin.memberProperties.filter { it.annotations.any { it.annotationClass == AutoProvide::class } }
                    .map { it as KMutableProperty1<Any, Any?>  }
                    .forEach { propList.add(it) }
        } else {
            propList = propertyCache[c]!!
        }

        propList.forEach {
            it.set(this, provideDependency(it.javaField!!.type))
        }
    }
}