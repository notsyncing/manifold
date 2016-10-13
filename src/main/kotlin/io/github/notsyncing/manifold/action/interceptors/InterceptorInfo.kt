package io.github.notsyncing.manifold.action.interceptors

abstract class InterceptorInfo<T: Interceptor<*>>(open val interceptorClass: Class<T>) {
}