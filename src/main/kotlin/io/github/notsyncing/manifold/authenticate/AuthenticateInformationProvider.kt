package io.github.notsyncing.manifold.authenticate

import java.util.concurrent.CompletableFuture

interface AuthenticateInformationProvider {
    fun getRole(id: String): CompletableFuture<AuthRole?>
}