package io.github.notsyncing.manifold.action.metadata

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SceneDescription(val value: String)