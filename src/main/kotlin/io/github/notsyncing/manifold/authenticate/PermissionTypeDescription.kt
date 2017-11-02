package io.github.notsyncing.manifold.authenticate

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class PermissionTypeDescription(val value: String,
                                           val group: String = "",
                                           val internal: Boolean = false)