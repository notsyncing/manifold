package io.github.notsyncing.manifold.authenticate

class Permission(val module: String,
                 val type: String,
                 val state: PermissionState = PermissionState.Undefined,
                 var additionalData: Any? = null,
                 var inherited: Boolean = false) {
    companion object {
        class PermissionBuilderItem(private val list: MutableList<Permission>, val state: PermissionState, val module: String) {
            constructor(list: MutableList<Permission>, state: PermissionState, module: Int) : this(list, state, module.toString())

            @Deprecated("Please use String to specify type")
            infix fun type(type: Enum<*>): Permission {
                val p = Permission(module, type.name, state)
                list.add(p)

                return p
            }

            infix fun type(type: String): Permission {
                val p = Permission(module, type, state)
                list.add(p)

                return p
            }

            @Deprecated("Please use String to specify types")
            infix fun type(types: Array<Enum<*>>): PermissionBuilderItem {
                types.forEach { type(it) }

                return this
            }

            infix fun type(types: Array<String>): PermissionBuilderItem {
                types.forEach { type(it) }

                return this
            }
        }

        class PermissionBuilderInner(private val list: MutableList<Permission>) {
            @Deprecated("Please use String to specify module")
            infix fun allow(module: Enum<*>): PermissionBuilderItem {
                return PermissionBuilderItem(list, PermissionState.Allowed, module.name)
            }

            infix fun allow(module: String): PermissionBuilderItem {
                return PermissionBuilderItem(list, PermissionState.Allowed, module)
            }

            @Deprecated("Please use String to specify modules")
            infix fun deny(module: Enum<*>): PermissionBuilderItem {
                return PermissionBuilderItem(list, PermissionState.Forbidden, module.name)
            }

            infix fun deny(module: String): PermissionBuilderItem {
                return PermissionBuilderItem(list, PermissionState.Forbidden, module)
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

    @Deprecated("Please use String to specify module and type")
    constructor(module: Int, type: Int, state: PermissionState = PermissionState.Undefined,
                additionalData: Any? = null, inherited: Boolean = false)
            : this(SceneAuthenticator.authModuleEnumClass!!.enumConstants[module].let { if (PermissionManager.useIntegerAsPermissions) it.ordinal.toString() else it.name },
            SceneAuthenticator.authTypeEnumClass!!.enumConstants[type].let { if (PermissionManager.useIntegerAsPermissions) it.ordinal.toString() else it.name },
            state, additionalData, inherited)

    @Deprecated("Please use String to specify module and type")
    constructor(module: Enum<*>, type: Enum<*>, state: PermissionState = PermissionState.Undefined,
                additionalData: Any? = null, inherited: Boolean = false)
            : this(module.let { if (PermissionManager.useIntegerAsPermissions) it.ordinal.toString() else it.name },
            type.let { if (PermissionManager.useIntegerAsPermissions) it.ordinal.toString() else it.name }, state,
            additionalData, inherited)

    constructor() : this(0, 0)

    infix fun additional(data: Any) {
        this.additionalData = data
    }
}