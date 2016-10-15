package io.github.notsyncing.manifold.action.interceptors

import io.github.notsyncing.manifold.action.ManifoldActionContextRunner

abstract class SceneInterceptor : Interceptor<SceneInterceptorContext>() {
    lateinit var m: ManifoldActionContextRunner
}