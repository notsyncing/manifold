package io.github.notsyncing.manifold.authenticate

class AggregatedPermissions(val permissions: List<Permission>) {
    fun get(module: Enum<*>, type: Enum<*>): Permission {
        val r = permissions.firstOrNull { (it.module == module) && (it.type == type) }

        if (r == null) {
            return Permission(module, type, PermissionState.Undefined)
        }

        return r
    }
}