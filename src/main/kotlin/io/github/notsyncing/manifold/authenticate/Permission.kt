package io.github.notsyncing.manifold.authenticate

class Permission(val module: Enum<*>,
                 val type: Enum<*>,
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
            : this(SceneAuthenticator.authModuleEnumClass.enumConstants[module],
                    SceneAuthenticator.authTypeEnumClass.enumConstants[type], state, additionalData, inherited) {
    }

    constructor() : this(0, 0)

    infix fun additional(data: Any) {
        this.additionalData = data
    }
}