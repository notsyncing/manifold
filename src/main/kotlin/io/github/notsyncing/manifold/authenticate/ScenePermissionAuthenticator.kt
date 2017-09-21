package io.github.notsyncing.manifold.authenticate

import io.github.notsyncing.manifold.action.interceptors.SceneInterceptorContext
import java.util.concurrent.CompletableFuture

open class ScenePermissionAuthenticator(private val authModule: String,
                                        private val authType: String) : SceneAuthenticator() {
    @Deprecated("Please use String to specify module and type")
    constructor(authModule: Enum<*>, authType: Enum<*>) : this(authModule.name, authType.name)

    override fun authenticate(context: SceneInterceptorContext, role: AuthRole): CompletableFuture<Unit> {
        return context.checkPermission(authModule, authType)
    }
}