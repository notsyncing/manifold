package io.github.notsyncing.manifold.tests.toys.authenticate

import io.github.notsyncing.manifold.action.interceptors.SceneInterceptorContext
import io.github.notsyncing.manifold.authenticate.AuthRole
import io.github.notsyncing.manifold.authenticate.Permission
import io.github.notsyncing.manifold.authenticate.SceneAuthenticator
import java.util.concurrent.CompletableFuture

class TestSceneAuthenticator : SceneAuthenticator() {
    companion object {
        var permission: Permission? = null

        fun reset() {
            permission = null
        }
    }

    override fun authenticate(context: SceneInterceptorContext, role: AuthRole): CompletableFuture<Unit> {
        val a = context.annotation as TestAuthAnno
        permission = context.getPermission(a.value, a.type)

        return context.checkPermission(a.value, a.type)
    }
}