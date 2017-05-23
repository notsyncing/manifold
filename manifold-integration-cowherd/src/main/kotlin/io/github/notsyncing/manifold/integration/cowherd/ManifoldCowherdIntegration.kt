package io.github.notsyncing.manifold.integration.cowherd

import io.github.notsyncing.cowherd.api.CowherdApiHub
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.feature.FeaturePublisher
import io.github.notsyncing.manifold.story.StoryScene

object ManifoldCowherdIntegration {
    private fun initFeaturePublisher() {
        Manifold.featurePublisher = object : FeaturePublisher {
            override fun publishFeature(sceneClass: Class<ManifoldScene<*>>) {
                CowherdApiHub.publish(sceneClass) {
                    ManifoldSceneApiExecutor(sceneClass)
                }

                if (sceneClass.isAnnotationPresent(SceneMetadata::class.java)) {
                    CowherdApiHub.publish(sceneClass.getAnnotation(SceneMetadata::class.java).value,
                            ManifoldSceneApiExecutor(sceneClass))
                }
            }
        }
    }

    private fun initActionPortal() {
        CowherdApiHub.publish(ActionPortalScene::class.java) {
            ManifoldSceneApiExecutor(ActionPortalScene::class.java as Class<ManifoldScene<*>>)
        }
    }

    private fun initStoryPortal() {
        CowherdApiHub.publish(StoryScene::class.java) {
            ManifoldStoryNarrator()
        }
    }

    fun init() {
        initFeaturePublisher()

        initActionPortal()

        initStoryPortal()
    }
}