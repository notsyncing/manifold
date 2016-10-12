package io.github.notsyncing.manifold.action.interceptors

import io.github.notsyncing.manifold.action.ManifoldAction

class ActionInterceptorContext(val action: ManifoldAction<*>) : InterceptorContext() {
}