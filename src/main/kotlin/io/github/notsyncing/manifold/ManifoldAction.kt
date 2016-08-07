package io.github.notsyncing.manifold

import io.github.notsyncing.manifold.annotations.AutoProvide
import kotlin.reflect.KMutableProperty
import kotlin.reflect.companionObject
import kotlin.reflect.companionObjectInstance
import kotlin.reflect.jvm.javaField
import kotlin.reflect.memberProperties

abstract class ManifoldAction<T>(private var managers: ManifoldManagerProvider? = Manifold.managerProvider) {
    companion object {

    }

    var transaction: T? = null

    init {
        val coClass = this.javaClass.kotlin.companionObject

        if (coClass != null) {
            coClass.memberProperties.filter {
                it.annotations.any {
                    it.annotationClass.equals(AutoProvide::class)
                }
            }.map {
                it as KMutableProperty<*>
            }.forEach {
                val o = managers?.get(it.javaField!!.type)

                if (o != null) {
                    it.setter.call(this.javaClass.kotlin.companionObjectInstance, o)
                }
            }
        }
    }

    fun with(trans: T): ManifoldAction<T> {
        transaction = trans
        return this
    }
}