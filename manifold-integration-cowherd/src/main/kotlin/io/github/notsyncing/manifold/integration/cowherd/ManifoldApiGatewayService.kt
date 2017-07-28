package io.github.notsyncing.manifold.integration.cowherd

import io.github.notsyncing.cowherd.annotations.Exported
import io.github.notsyncing.cowherd.annotations.Parameter
import io.github.notsyncing.cowherd.annotations.Route
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpAnyMethod
import io.github.notsyncing.cowherd.api.CowherdApiGatewayService
import io.github.notsyncing.cowherd.api.CowherdApiHub
import io.github.notsyncing.cowherd.models.ActionContext
import io.github.notsyncing.cowherd.models.Pair
import io.github.notsyncing.cowherd.models.UploadFileInfo
import io.github.notsyncing.manifold.Manifold
import io.vertx.core.http.HttpServerRequest
import java.net.HttpCookie
import java.util.concurrent.CompletableFuture

class ManifoldApiGatewayService : CowherdApiGatewayService() {
    @HttpAnyMethod
    @Exported
    @Route("", subRoute = true)
    override fun gateway(@Parameter("path") path: String,
                         @Parameter("request") request: HttpServerRequest?,
                         @Parameter("context") context: ActionContext,
                         @Parameter("__parameters__") __parameters__: List<Pair<String, String>>,
                         @Parameter("__cookies__") __cookies__: List<HttpCookie>?,
                         @Parameter("__uploads__") __uploads__: List<UploadFileInfo>?): CompletableFuture<Any?> {
        return super.gateway(path, request, context, __parameters__, __cookies__, __uploads__)
    }

    override fun resolveNamespace(namespace: String, serviceClassName: String): String {
        var domainAwareName = namespace + "_" + serviceClassName
        var currentDomain = Manifold.rootDomain.findDomain(namespace)?.get()

        while (!CowherdApiHub.has(domainAwareName)) {
            currentDomain = currentDomain?.parentDomain

            if (currentDomain == null) {
                return serviceClassName
            }

            domainAwareName = currentDomain.name + "_" + serviceClassName
        }

        return domainAwareName
    }
}