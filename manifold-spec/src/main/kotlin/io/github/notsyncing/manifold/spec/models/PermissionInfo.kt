package io.github.notsyncing.manifold.spec.models

import io.github.notsyncing.manifold.feature.DefaultAuth
import io.github.notsyncing.manifold.feature.DefaultModule

class PermissionInfo {
    var module: Enum<*> = DefaultModule.Undefined
    var type: Enum<*> = DefaultAuth.Undefined

    infix fun needs(module: Enum<*>): PermissionInfo {
        this.module = module
        return this
    }

    infix fun type(type: Enum<*>): PermissionInfo {
        this.type = type
        return this
    }
}