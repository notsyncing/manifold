package io.github.notsyncing.manifold.spec.models

class PermissionInfo {
    var module: Enum<*>? = null
    var type: Enum<*>? = null

    infix fun needs(module: Enum<*>): PermissionInfo {
        this.module = module
        return this
    }

    infix fun type(type: Enum<*>): PermissionInfo {
        this.type = type
        return this
    }
}