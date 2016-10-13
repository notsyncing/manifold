package io.github.notsyncing.manifold.action.interceptors

class ActionInterceptorInfo(override val interceptorClass: Class<ActionInterceptor>,
                            val forAnnotation: Annotation? = null) : InterceptorInfo<ActionInterceptor>(interceptorClass) {
}