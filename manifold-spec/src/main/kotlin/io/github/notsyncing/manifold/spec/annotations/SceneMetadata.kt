package io.github.notsyncing.manifold.spec.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SceneMetadata(val value: String)