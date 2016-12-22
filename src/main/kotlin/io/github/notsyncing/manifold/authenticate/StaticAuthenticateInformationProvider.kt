package io.github.notsyncing.manifold.authenticate

import java.util.concurrent.CompletableFuture

class StaticAuthenticateInformationProvider(private val role: AuthRole) : AuthenticateInformationProvider {
    override fun getRole(id: String): CompletableFuture<AuthRole?> {
        return CompletableFuture.completedFuture(role)
    }
}