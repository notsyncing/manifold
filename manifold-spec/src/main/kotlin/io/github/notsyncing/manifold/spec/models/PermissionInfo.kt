package io.github.notsyncing.manifold.spec.models

class PermissionInfo {
    var module: String = "manifold.modules.undefined"
        private set

    var type: String = "manifold.type.undefined"
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