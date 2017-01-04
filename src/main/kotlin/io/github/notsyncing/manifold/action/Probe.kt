package io.github.notsyncing.manifold.action

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Probe(val value: String = "")