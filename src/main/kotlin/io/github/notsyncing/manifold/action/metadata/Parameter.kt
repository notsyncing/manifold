package io.github.notsyncing.manifold.action.metadata

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Parameter(val value: String, val description: String = "")