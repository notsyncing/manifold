package io.github.notsyncing.manifold.integration.cowherd

import io.github.notsyncing.cowherd.CowherdPart
import io.github.notsyncing.cowherd.api.CowherdApiHub
import io.github.notsyncing.cowherd.commons.CowherdConfiguration
import io.github.notsyncing.cowherd.models.RouteInfo
import io.github.notsyncing.cowherd.service.ServiceManager
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.domain.DomainClassLoader
import io.github.notsyncing.manifold.domain.ManifoldDomain
import io.github.notsyncing.manifold.feature.FeaturePublisher
import io.github.notsyncing.manifold.story.StoryScene
import io.vertx.core.json.JsonObject

class ManifoldCowherdIntegrationPart : CowherdPart {
    private val defConfig = JsonObject()

    init {
        defConfig.put("urlPrefix", "manifold")
    }

    override fun init() {
        initFeaturePublisher()

        initActionPortal()

        initStoryPortal()

        var config = CowherdConfiguration.getRawConfiguration().getJsonObject("manifoldApi")

        if (config == null) {
            config = defConfig
        }

        val apiRoute = RouteInfo()
        apiRoute.path = "/${config.getString("urlPrefix")}/gateway/**:path"
        apiRoute.domain = config.getString("domain")
        apiRoute.isFastRoute = true

        ServiceManager.addServiceClass(ManifoldApiGatewayService::class.java, apiRoute)

        ManifoldDomain.onClose { _, cl ->
            ManifoldSceneApiExecutor.removeFromCacheIf { it.classLoader == cl }
        }
    }

    override fun destroy() {
        CowherdApiHub.reset()
    }

    private fun initFeaturePublisher() {
        Manifold.featurePublisher = object : FeaturePublisher {
            private fun getDomainName(sceneClass: Class<ManifoldScene<*>>): String {
                var domainName = ""
                val domainClassLoader = findParentDomainClassLoader(sceneClass)

                if ((domainClassLoader != null) && (domainClassLoader.domainName != ManifoldDomain.ROOT)) {
                    domainName = domainClassLoader.domainName + "_"
                }

                return domainName
            }

            override fun publishFeature(sceneClass: Class<ManifoldScene<*>>) {
                val domainName = getDomainName(sceneClass)

                CowherdApiHub.publish(domainName + sceneClass.name, ManifoldSceneApiExecutor(sceneClass))

                if (sceneClass.isAnnotationPresent(SceneMetadata::class.java)) {
                    val serviceName = domainName + sceneClass.getAnnotation(SceneMetadata::class.java).value

                    CowherdApiHub.publish(serviceName, ManifoldSceneApiExecutor(sceneClass))
                }
            }

            override fun revokeFeature(sceneClass: Class<ManifoldScene<*>>) {
                val domainName = getDomainName(sceneClass)

                CowherdApiHub.revoke(domainName + sceneClass.name)

                if (sceneClass.isAnnotationPresent(SceneMetadata::class.java)) {
                    val serviceName = domainName + sceneClass.getAnnotation(SceneMetadata::class.java).value

                    CowherdApiHub.revoke(serviceName)
                }
            }
        }
    }

    private fun findParentDomainClassLoader(clazz: Class<*>, currClassLoader: ClassLoader? = clazz.classLoader): DomainClassLoader? {
        if (currClassLoader == null) {
            return null
        } else if (currClassLoader is DomainClassLoader) {
            return currClassLoader
        }

        return findParentDomainClassLoader(clazz, currClassLoader.parent)
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
}