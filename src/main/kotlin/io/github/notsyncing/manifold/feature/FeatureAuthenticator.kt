package io.github.notsyncing.manifold.feature

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.ForEveryScene
import io.github.notsyncing.manifold.action.interceptors.SceneInterceptorContext
import io.github.notsyncing.manifold.authenticate.AuthRole
import io.github.notsyncing.manifold.authenticate.SceneAuthenticator
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@ForEveryScene
class FeatureAuthenticator : SceneAuthenticator() {
    companion object {
        private val featureAuthMap = ConcurrentHashMap<String, Pair<Enum<*>, Enum<*>>>()

        class FeatureAuthMapItem(val authModule: Enum<*>) {
            lateinit var authType: Enum<*>
            lateinit var feature: String

            infix fun type(authType: Enum<*>): FeatureAuthMapItem {
                this.authType = authType
                return this
            }

            infix fun to(feature: String): FeatureAuthMapItem {
                this.feature = feature
                featureAuthMap[feature] = Pair(authModule, authType)
                return this
            }
        }

        class FeatureAuthBuilder {
            companion object {
                val lets = FeatureAuthBuilder()
            }

            val lets = Companion.lets

            infix fun map(authModule: Enum<*>): FeatureAuthMapItem {
                return FeatureAuthMapItem(authModule)
            }
        }

        fun configure(conf: FeatureAuthBuilder.() -> Unit) {
            FeatureAuthBuilder().conf()
        }

        fun reset() {
            featureAuthMap.clear()
        }
    }

    override fun authenticate(context: SceneInterceptorContext, role: AuthRole): CompletableFuture<Unit> {
        if (!Manifold.enableFeatureManagement) {
            return context.pass()
        }

        val feature = context.scene.javaClass.getAnnotation(Feature::class.java)

        if (feature == null) {
            return context.pass()
        }

        val (module, type) = featureAuthMap[feature.value] ?: Pair(null, null)

        if ((module == null) || (type == null)) {
            return context.pass()
        }

        return context.checkPermission(module, type)
    }
}