package io.github.notsyncing.manifold.action.session

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldAction
import kotlin.reflect.KProperty

class SessionVariable<T>(val name: String? = null) {
    operator fun getValue(thisRef: ManifoldAction<*, *>?, property: KProperty<*>): T? {
        if (thisRef?.sessionIdentifier == null) {
            return null
        }

        return Manifold.sessionStorageProvider?.get<T>(thisRef!!.sessionIdentifier!!, name ?: property.name)
    }

    operator fun setValue(thisRef: ManifoldAction<*, *>?, property: KProperty<*>, value: T?) {
        if (thisRef?.sessionIdentifier == null) {
            return
        }

        if (value == null) {
            Manifold.sessionStorageProvider?.remove<T>(thisRef!!.sessionIdentifier!!, name ?: property.name)
        } else {
            Manifold.sessionStorageProvider?.put<T>(thisRef!!.sessionIdentifier!!, name ?: property.name, value as Any)
        }
    }
}