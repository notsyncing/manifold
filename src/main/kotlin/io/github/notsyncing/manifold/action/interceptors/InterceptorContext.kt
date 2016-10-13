package io.github.notsyncing.manifold.action.interceptors

abstract class InterceptorContext {
    var annotation: Annotation? = null
    var result: Any? = null
    var interceptorResult = InterceptorResult.Continue
}