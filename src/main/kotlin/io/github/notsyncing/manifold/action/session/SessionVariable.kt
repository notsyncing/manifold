package io.github.notsyncing.manifold.action.session

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.ManifoldScene
import kotlin.reflect.KProperty

class SessionVariable<T>(val name: String? = null) {
    private fun getValue(sessionId: String?, property: KProperty<*>): T? {
        if (sessionId == null) {
            return null
        }

        return Manifold.sessionStorageProvider?.get<T>(sessionId, name ?: property.name)
    }

    private fun setValue(sessionId: String?, property: KProperty<*>, value: T?) {
        if (sessionId == null) {
            return
        }

        if (value == null) {
            Manifold.sessionStorageProvider?.remove<T>(sessionId, name ?: property.name)
        } else {
            Manifold.sessionStorageProvider?.put<T>(sessionId, name ?: property.name, value as Any)
        }
    }

    operator fun getValue(thisRef: ManifoldAction<*, *>?, property: KProperty<*>): T? {
        return getValue(thisRef!!.sessionIdentifier, property)
    }

    operator fun setValue(thisRef: ManifoldAction<*, *>?, property: KProperty<*>, value: T?) {
        setValue(thisRef!!.sessionIdentifier!!, property, value)
    }

    operator fun getValue(thisRef: ManifoldScene<*>?, property: KProperty<*>): T? {
        return getValue(thisRef!!.sessionIdentifier, property)
    }

    operator fun setValue(thisRef: ManifoldScene<*>?, property: KProperty<*>, value: T?) {
        setValue(thisRef!!.sessionIdentifier!!, property, value)
    }
}