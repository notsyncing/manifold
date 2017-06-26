package io.github.notsyncing.manifold.story.stateful

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class StateMetadata(val value: String, val description: String)