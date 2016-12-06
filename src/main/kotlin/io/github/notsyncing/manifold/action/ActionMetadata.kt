package io.github.notsyncing.manifold.action

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActionMetadata(val value: String)