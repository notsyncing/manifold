package io.github.notsyncing.manifold.story

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AfterStoryOf(val value: Array<String>)