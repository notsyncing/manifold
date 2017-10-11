package io.github.notsyncing.manifold.hooking

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookingFor(val value: String)