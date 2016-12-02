package io.github.notsyncing.manifold.spec.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActionMetadata(val value: String)