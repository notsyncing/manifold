package io.github.notsyncing.manifold.authenticate

class Permission(val module: Enum<*>,
                 val type: Enum<*>,
                 val state: PermissionState = PermissionState.Undefined,
                 val additionalData: Any? = null) {
    constructor(module: Int, type: Int, state: PermissionState = PermissionState.Undefined, additionalData: Any? = null)
            : this(SceneAuthenticator.authModuleEnumClass.enumConstants[module],
                    SceneAuthenticator.authTypeEnumClass.enumConstants[type], state, additionalData) {

    }
}