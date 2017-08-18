package io.github.notsyncing.manifold.feature

import io.github.notsyncing.manifold.action.ManifoldScene

class FeatureInfo(val sceneClass: Class<ManifoldScene<*>>, val name: String,
                  val groups: Array<String> = arrayOf(), val successorOf: String = "",
                  val internal: Boolean = false, val defaultSpecialAuths: Array<String> = arrayOf()) {
    companion object {
        fun from(sceneClass: Class<ManifoldScene<*>>): FeatureInfo {
            val f = sceneClass.getAnnotation(Feature::class.java)
            return FeatureInfo(sceneClass, f.value, f.groups, f.successorOf, f.internal, f.defaultSpecialAuths)
        }
    }

    var initialized = false
    var published = false
}