package io.github.notsyncing.manifold

import kotlin.reflect.KMutableProperty
import kotlin.reflect.companionObject
import kotlin.reflect.companionObjectInstance
import kotlin.reflect.jvm.javaField
import kotlin.reflect.memberProperties

abstract class ManifoldAction<T>() {
    var transaction: T? = null

    fun with(trans: T): ManifoldAction<T> {
        transaction = trans
        return this
    }
}