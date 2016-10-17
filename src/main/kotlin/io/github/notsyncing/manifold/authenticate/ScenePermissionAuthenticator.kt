package io.github.notsyncing.manifold.authenticate

import io.github.notsyncing.manifold.action.interceptors.SceneInterceptorContext
import java.util.concurrent.CompletableFuture

open class ScenePermissionAuthenticator(private val authModule: Enum<*>,
                                        private val authType: Enum<*>) : SceneAuthenticator() {
    override fun authenticate(context: SceneInterceptorContext): CompletableFuture<Unit> {
        return context.checkPermission(authModule, authType)
    }
}