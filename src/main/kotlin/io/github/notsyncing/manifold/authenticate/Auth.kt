package io.github.notsyncing.manifold.authenticate

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.utils.FutureUtils
import java.net.HttpCookie
import java.util.concurrent.CompletableFuture

object Auth {
    fun validate(id: String?, module: String, type: String, onDisallowed: () -> Unit = {}): CompletableFuture<Pair<AuthRole, Permission>> {
        if ((id == null) || (Manifold.authInfoProvider == null)) {
            try {
                onDisallowed()
            } catch (e: Exception) {
                return FutureUtils.failed(e)
            }

            return CompletableFuture.completedFuture(Pair(AuthRole(), Permission()))
        }

        return Manifold.authInfoProvider!!.getRole(id)
                .thenApply {
                    if (it == null) {
                        onDisallowed()

                        return@thenApply Pair(AuthRole(), Permission())
                    }

                    val permissions = SceneAuthenticator.aggregatePermissions(it)
                    val permission = permissions.get(module, type)

                    if (permission.disallowed()) {
                        onDisallowed()
                    }

                    Pair(it, permission)
                }
    }

    fun validate(id: HttpCookie?, module: String, type: String, onDisallowed: () -> Unit = {}): CompletableFuture<Pair<AuthRole, Permission>> {
        return validate(id?.value, module, type, onDisallowed)
    }
}