package io.github.notsyncing.manifold.feature

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.vertx.core.impl.ConcurrentHashSet
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class FeatureManager {
    var enableFeatureManagement = true
    var featurePublisher: FeaturePublisher? = null

    private val enabledFeatures = ConcurrentHashSet<String>()
    private val enabledFeatureGroups = ConcurrentHashSet<String>()
    private val disabledFeatures = ConcurrentHashSet<String>()
    private val disabledFeatureGroups = ConcurrentHashSet<String>()

    private val allFeatures = ConcurrentHashMap<Class<ManifoldScene<*>>, FeatureInfo>()
    private val availableFeatures = ArrayList<FeatureInfo>()

    private var featurePublished = false

    fun destroy() {
        enabledFeatures.clear()
        enabledFeatureGroups.clear()
        disabledFeatures.clear()
        disabledFeatureGroups.clear()

        allFeatures.clear()
        availableFeatures.clear()

        availableFeatures.forEach { it.sceneClass.newInstance().destroy() }

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
            allFeatures.filterValues { isFeatureEnabled(it) }
                    .filterValues { v -> !allFeatures.any { it.value.successorOf == v.name } }
                    .forEach { c, info -> availableFeatures.add(info) }
        } else {
            allFeatures.forEach { availableFeatures.add(it.value) }
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
            // TODO: Support dynamic feature enabling
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
        if (!Manifold.enableFeatureManagement) {
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
        if (featurePublished) {
            return isFeatureEnabled(allFeatures[sceneClass as Class<ManifoldScene<*>>]!!) && availableFeatures.any { it.sceneClass == sceneClass }
        } else {
            return isFeatureEnabled(FeatureInfo.from(sceneClass as Class<ManifoldScene<*>>))
        }
    }

    fun publishFeature(sceneClass: Class<ManifoldScene<*>>) {
        if (featurePublisher != null) {
            featurePublisher!!.publishFeature(sceneClass)
        }
    }

    fun publishFeatures() {
        initEnabledFeatures()

        availableFeatures.forEach {
            try {
                it.sceneClass.newInstance().init()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            publishFeature(it.sceneClass)
        }

        featurePublished = true
    }
}