package io.github.notsyncing.manifold.integration.cowherd

import io.github.notsyncing.cowherd.api.CowherdApiHub
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.feature.FeaturePublisher

object ManifoldCowherdIntegration {
    fun init() {
        Manifold.featurePublisher = object : FeaturePublisher {
            override fun publishFeature(sceneClass: Class<ManifoldScene<*>>) {
                CowherdApiHub.publish(sceneClass) {
                    ManifoldSceneApiExecutor(sceneClass)
                }
            }
        }
    }
}