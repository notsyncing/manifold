package io.github.notsyncing.manifold.feature

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Feature(val value: String, val groups: Array<String> = arrayOf())