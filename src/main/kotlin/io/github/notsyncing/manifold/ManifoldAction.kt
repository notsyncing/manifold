package io.github.notsyncing.manifold

import java.util.concurrent.CompletableFuture
import kotlin.reflect.KMutableProperty
import kotlin.reflect.companionObject
import kotlin.reflect.companionObjectInstance
import kotlin.reflect.jvm.javaField
import kotlin.reflect.memberProperties

abstract class ManifoldAction<T, R>() {
    var transaction: T? = null

    fun with(trans: T?): ManifoldAction<T, R> {
        transaction = trans
        return this
    }

    fun execute(f: (ManifoldAction<T, R>) -> CompletableFuture<R>): CompletableFuture<R> {
        return f(this)
    }
}