package io.github.notsyncing.manifold.spec.models

import io.github.notsyncing.manifold.feature.FeatureAuthenticator.Companion.PERMISSION_MODULE_UNDEFINED
import io.github.notsyncing.manifold.feature.FeatureAuthenticator.Companion.PERMISSION_TYPE_UNDEFINED

class PermissionInfo {
    var module: String = PERMISSION_MODULE_UNDEFINED
        private set

    var type: String = PERMISSION_TYPE_UNDEFINED
        private set

    @Deprecated("Please use String to specify module")
    infix fun needs(module: Enum<*>): PermissionInfo {
        return needs(module.name)
    }

    infix fun needs(module: String): PermissionInfo {
        this.module = module
        return this
    }

    @Deprecated("Please use String to specify type")
    infix fun type(type: Enum<*>): PermissionInfo {
        return type(type.name)
    }

    infix fun type(type: String): PermissionInfo {
        this.type = type
        return this
    }
}