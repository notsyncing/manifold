package io.github.notsyncing.manifold.management

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ManagementCommand(val value: String = "")