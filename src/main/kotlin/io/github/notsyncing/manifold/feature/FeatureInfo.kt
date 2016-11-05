package io.github.notsyncing.manifold.feature

import io.github.notsyncing.manifold.action.ManifoldScene

class FeatureInfo(val sceneClass: Class<ManifoldScene<*>>, val name: String,
                  val groups: Array<String> = arrayOf(), val successorOf: String = "") {
    companion object {
        fun from(sceneClass: Class<ManifoldScene<*>>): FeatureInfo {
            val f = sceneClass.getAnnotation(Feature::class.java)
            return FeatureInfo(sceneClass, f.value, f.groups, f.successorOf)
        }
    }
}