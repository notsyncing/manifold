package io.github.notsyncing.manifold.authenticate

import io.github.notsyncing.manifold.action.interceptors.SceneInterceptorContext

class NoPermissionException(msg: String,
                            val context: SceneInterceptorContext) : Exception(msg) {

}