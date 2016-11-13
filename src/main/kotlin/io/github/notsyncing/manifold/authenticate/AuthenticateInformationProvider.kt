package io.github.notsyncing.manifold.authenticate

import java.util.concurrent.CompletableFuture

interface AuthenticateInformationProvider {
    fun getRole(id: String): CompletableFuture<AuthRole?>

    fun destroy(): CompletableFuture<Unit> {
        return CompletableFuture.completedFuture(Unit)
    }
}