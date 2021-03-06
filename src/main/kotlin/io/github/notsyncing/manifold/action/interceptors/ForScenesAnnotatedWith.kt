package io.github.notsyncing.manifold.action.interceptors

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ForScenesAnnotatedWith(vararg val value: KClass<*>)