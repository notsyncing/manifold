package io.github.notsyncing.manifold.feature

import io.github.notsyncing.manifold.action.ManifoldScene

interface FeaturePublisher {
    fun publishFeature(sceneClass: Class<ManifoldScene<*>>)

    fun revokeFeature(sceneClass: Class<ManifoldScene<*>>)
}