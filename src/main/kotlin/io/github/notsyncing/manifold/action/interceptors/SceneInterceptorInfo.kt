package io.github.notsyncing.manifold.action.interceptors

class SceneInterceptorInfo(override val interceptorClass: Class<SceneInterceptor>,
                           val forAnnotation: Annotation? = null) : InterceptorInfo<SceneInterceptor>(interceptorClass) {
}