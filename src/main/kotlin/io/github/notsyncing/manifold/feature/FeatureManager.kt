package io.github.notsyncing.manifold.feature

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.vertx.core.impl.ConcurrentHashSet

class FeatureManager {
    var enableFeatureManagement = true
    var featurePublisher: FeaturePublisher? = null

    private val enabledFeatures = ConcurrentHashSet<String>()
    private val enabledFeatureGroups = ConcurrentHashSet<String>()
    private val disabledFeatures = ConcurrentHashSet<String>()
    private val disabledFeatureGroups = ConcurrentHashSet<String>()

    fun reset() {
        enabledFeatures.clear()
        enabledFeatureGroups.clear()
        disabledFeatures.clear()
        disabledFeatureGroups.clear()
    }

    fun enableFeatures(vararg features: String) {
        features.forEach { enabledFeatures.add(it) }
    }

    fun disableFeatures(vararg features: String) {
        features.forEach {
            enabledFeatures.remove(it)
            disabledFeatures.add(it)
        }
    }

    fun enableFeatureGroups(vararg featureGroups: String) {
        featureGroups.forEach { enabledFeatureGroups.add(it) }
    }

    fun disableFeatureGroups(vararg featureGroups: String) {
        featureGroups.forEach {
            enabledFeatureGroups.remove(it)
            disabledFeatureGroups.add(it)
        }
    }

    fun <T: ManifoldScene<*>> isFeatureEnabled(sceneClass: Class<T>): Boolean {
        if (!Manifold.enableFeatureManagement) {
            return true
        }

        val featureAnno = sceneClass.getAnnotation(Feature::class.java)

        if (featureAnno == null) {
            return false
        }

        if (disabledFeatures.contains(featureAnno.value)) {
            return false
        }

        if (featureAnno.groups.any { disabledFeatureGroups.contains(it) }) {
            return false
        }

        if (enabledFeatures.contains(featureAnno.value)) {
            return true
        }

        if (featureAnno.groups.any { enabledFeatureGroups.contains(it) }) {
            return true
        }

        return false
    }

    fun publishFeature(sceneClass: Class<ManifoldScene<*>>) {
        if (featurePublisher != null) {
            featurePublisher!!.publishFeature(sceneClass)
        }
    }
}