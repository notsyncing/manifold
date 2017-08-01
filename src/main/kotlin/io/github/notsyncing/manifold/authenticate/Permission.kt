package io.github.notsyncing.manifold.authenticate

import io.github.notsyncing.manifold.Manifold
import java.security.InvalidParameterException

class Permission(val module: String,
                 val type: String,
                 val state: PermissionState = PermissionState.Undefined,
                 var additionalData: Any? = null,
                 var inherited: Boolean = false) {
    companion object {
        class PermissionBuilderItem(private val list: MutableList<Permission>, val state: PermissionState, val module: Int) {
            infix fun type(type: Enum<*>): Permission {
                val p = Permission(module, type.ordinal, state)
                list.add(p)

                return p
            }

            infix fun type(types: Array<Enum<*>>): PermissionBuilderItem {
                types.forEach { type(it) }

                return this
            }
        }

        class PermissionBuilderInner(private val list: MutableList<Permission>) {
            infix fun allow(module: Enum<*>): PermissionBuilderItem {
                return PermissionBuilderItem(list, PermissionState.Allowed, module.ordinal)
            }

            infix fun deny(module: Enum<*>): PermissionBuilderItem {
                return PermissionBuilderItem(list, PermissionState.Forbidden, module.ordinal)
            }
        }

        class PermissionBuilder {
            val list = mutableListOf<Permission>()
            val lets = PermissionBuilderInner(list)
        }

        fun build(b: PermissionBuilder.() -> Unit): List<Permission> {
            val builder = PermissionBuilder()
            builder.b()

            return builder.list
        }
    }

    constructor(module: Int, type: Int, state: PermissionState = PermissionState.Undefined,
                additionalData: Any? = null, inherited: Boolean = false)
            : this(if (module >= 0) SceneAuthenticator.authModuleEnumClass.enumConstants[module].name else Manifold.permissions.getCustomPermission(module)?.name ?: throw InvalidParameterException("Unknown permission module $module"),
            if (type >= 0) SceneAuthenticator.authTypeEnumClass.enumConstants[type].name else "", state, additionalData,
            inherited)

    constructor(module: Enum<*>, type: Enum<*>, state: PermissionState = PermissionState.Undefined,
                additionalData: Any? = null, inherited: Boolean = false)
            : this(module.name, type.name, state, additionalData, inherited)

    constructor() : this(0, 0)

    infix fun additional(data: Any) {
        this.additionalData = data
    }
}