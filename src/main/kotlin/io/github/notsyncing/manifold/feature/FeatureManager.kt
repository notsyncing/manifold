package io.github.notsyncing.manifold.feature

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.domain.ManifoldDomain
import io.github.notsyncing.manifold.utils.removeIf
import java.util.concurrent.ConcurrentHashMap

class FeatureManager {
    var enableFeatureManagement = true
    var featurePublisher: FeaturePublisher? = null

    private val enabledFeatures = ConcurrentHashMap.newKeySet<String>()
    private val enabledFeatureGroups = ConcurrentHashMap.newKeySet<String>()
    private val disabledFeatures = ConcurrentHashMap.newKeySet<String>()
    private val disabledFeatureGroups = ConcurrentHashMap.newKeySet<String>()

    private val allFeatures = ConcurrentHashMap<Class<ManifoldScene<*>>, FeatureInfo>()
    private val availableFeatures = ConcurrentHashMap<Class<ManifoldScene<*>>, FeatureInfo>()

    private var featurePublished = false

    fun init() {
        ManifoldDomain.onClose { _, classLoader ->
            allFeatures.removeIf { (clazz, _) ->
                if (clazz.classLoader == classLoader) {
                    featurePublisher?.revokeFeature(clazz)
                    true
                } else {
                    false
                }
            }

            availableFeatures.removeIf { (clazz, _) -> clazz.classLoader == classLoader }
        }
    }

    fun destroy() {
        FeatureAuthenticator.reset()

        enabledFeatures.clear()
        enabledFeatureGroups.clear()
        disabledFeatures.clear()
        disabledFeatureGroups.clear()

        allFeatures.clear()
        availableFeatures.clear()

        availableFeatures.forEachValue(1) { it.sceneClass.newInstance().destroy() }

        featurePublished = false
    }

    fun registerFeature(s: Class<ManifoldScene<*>>) {
        if (enableFeatureManagement) {
            if (!s.isAnnotationPresent(Feature::class.java)) {
                return
            }

            val info = FeatureInfo.from(s)
            allFeatures[s] = info
        } else {
            allFeatures[s] = FeatureInfo(s, "")
        }
    }

    fun initEnabledFeatures() {
        if (enableFeatureManagement) {
            allFeatures.filterKeys { !availableFeatures.containsKey(it) }
                    .filterValues { isFeatureEnabled(it) }
                    .filterValues { v -> !allFeatures.any { it.value.successorOf == v.name } }
                    .forEach { c, info -> availableFeatures[c] = info }
        } else {
            allFeatures.filterKeys { !availableFeatures.containsKey(it) }
                    .forEach { availableFeatures[it.key] = it.value }
        }
    }

    fun enableFeatures(vararg features: String) {
        features.forEach { enabledFeatures.add(it) }

        if (featurePublished) {
            // TODO: Support dynamic feature enabling
        }
    }

    fun disableFeatures(vararg features: String) {
        features.forEach {
            enabledFeatures.remove(it)
            disabledFeatures.add(it)
        }

        if (featurePublished) {
            // TODO: Support dynamic feature disabling
        }
    }

    fun enableFeatureGroups(vararg featureGroups: String) {
        featureGroups.forEach { enabledFeatureGroups.add(it) }

        if (featurePublished) {
            publishFeatures()
        }
    }

    fun disableFeatureGroups(vararg featureGroups: String) {
        featureGroups.forEach {
            enabledFeatureGroups.remove(it)
            disabledFeatureGroups.add(it)
        }

        if (featurePublished) {
            // TODO: Support dynamic feature disabling
        }
    }

    fun isFeatureEnabled(info: FeatureInfo): Boolean {
        if (!enableFeatureManagement) {
            return true
        }

        if (disabledFeatures.contains(info.name)) {
            return false
        }

        if (info.groups.any { disabledFeatureGroups.contains(it) }) {
            return false
        }

        if (enabledFeatures.contains(info.name)) {
            return true
        }

        if (info.groups.any { enabledFeatureGroups.contains(it) }) {
            return true
        }

        return false
    }

    fun <T: ManifoldScene<*>> isFeatureEnabled(sceneClass: Class<T>): Boolean {
        if (!enableFeatureManagement) {
            return true
        }

        val c = sceneClass as Class<ManifoldScene<*>>

        if (featurePublished) {
            if (!allFeatures.containsKey(c)) {
                return true
            }

            return isFeatureEnabled(allFeatures[c]!!) && availableFeatures.containsKey(sceneClass)
        } else {
            return isFeatureEnabled(FeatureInfo.from(c))
        }
    }

    fun publishFeature(sceneClass: Class<ManifoldScene<*>>) {
        if (featurePublisher != null) {
            featurePublisher!!.publishFeature(sceneClass)
        }
    }

    fun publishFeatures() {
        initEnabledFeatures()

        availableFeatures.forEach { sceneClass, info ->
            if (!info.initialized) {
                try {
                    sceneClass.newInstance().init()
                    info.initialized = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (!info.internal) {
                if (!info.published) {
                    publishFeature(sceneClass)
                    info.published = true
                }
            }
        }

        featurePublished = true
    }

    fun republishAllFeatures() {
        availableFeatures.forEach { sceneClass, info ->
            if (!info.internal) {
                publishFeature(sceneClass)
            }
        }
    }
}