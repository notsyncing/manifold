package io.github.notsyncing.manifold.feature

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.ForEveryScene
import io.github.notsyncing.manifold.action.interceptors.SceneInterceptorContext
import io.github.notsyncing.manifold.authenticate.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@ForEveryScene
class FeatureAuthenticator : SceneAuthenticator() {
    companion object {
        private val featureAuthMap = ConcurrentHashMap<String, Pair<String, String>>()

        class FeatureAuthMapItem(val features: Array<String>) {
            lateinit var authModule: String
            lateinit var authType: String

            @Deprecated("Please use String to specify module")
            infix fun needs(module: Enum<*>): FeatureAuthMapItem {
                if (PermissionManager.useIntegerAsPermissions) {
                    return needs(module.ordinal.toString())
                } else {
                    return needs(module.name)
                }
            }

            infix fun needs(module: String): FeatureAuthMapItem {
                this.authModule = module
                return this
            }

            @Deprecated("Please use String to specify type")
            infix fun type(type: Enum<*>): FeatureAuthMapItem {
                if (PermissionManager.useIntegerAsPermissions) {
                    return type(type.ordinal.toString())
                } else {
                    return type(type.name)
                }
            }

            infix fun type(type: String): FeatureAuthMapItem {
                this.authType = type

                for (f in features) {
                    featureAuthMap[f] = Pair(authModule, authType)
                }

                return this
            }
        }

        class FeatureAuthBuilder {
            companion object {
                val our = FeatureAuthBuilder()

                val login = SpecialAuth.LoginOnly

                val noAuth = SpecialAuth.NoAuth
            }

            val our = Companion.our

            infix fun feature(feature: String): FeatureAuthMapItem {
                return FeatureAuthMapItem(arrayOf(feature))
            }

            infix fun feature(features: Array<String>): FeatureAuthMapItem {
                return FeatureAuthMapItem(features)
            }
        }

        fun configure(conf: FeatureAuthBuilder.() -> Unit) {
            FeatureAuthBuilder().conf()
        }

        fun reset() {
            featureAuthMap.clear()
        }

        fun getAuth(feature: String): Pair<String, String> {
            return featureAuthMap[feature] ?: Pair("manifold.modules.undefined", "manifold.types.undefined")
        }
    }

    private fun checkSpecialAuths(context: SceneInterceptorContext, role: AuthRole, auths: Array<String>): CompletableFuture<Unit> {
        for (a in auths) {
            if (a == SpecialAuth.LoginOnly) {
                if (role.roleId.isEmpty()) {
                    return context.deny()
                }
            }
        }

        return context.pass()
    }

    override fun authenticate(context: SceneInterceptorContext, role: AuthRole): CompletableFuture<Unit> {
        if (!Manifold.enableFeatureManagement) {
            return context.pass()
        }

        val feature = context.scene::class.java.getAnnotation(Feature::class.java)

        if (feature == null) {
            return context.pass()
        }

        if (role == SpecialRole.SuperUser) {
            return context.pass()
        }

        val (module, type) = featureAuthMap[feature.value] ?: Pair(null, null)

        if (module == SpecialAuth.NoAuth) {
            return context.pass()
        }

        if ((module == null) || (type == null)) {
            if (feature.defaultSpecialAuths.isNotEmpty()) {
                return checkSpecialAuths(context, role, feature.defaultSpecialAuths)
            } else {
                return context.pass()
            }
        }

        if (module == SpecialAuth.LoginOnly) {
            return if (role.roleId.isNotEmpty()) context.pass() else context.deny()
        }

        return context.checkPermission(module, type)
    }
}