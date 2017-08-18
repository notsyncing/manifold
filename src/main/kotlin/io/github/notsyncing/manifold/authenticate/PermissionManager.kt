package io.github.notsyncing.manifold.authenticate

import java.util.concurrent.ConcurrentHashMap

class PermissionManager {
    private val customPermissions = ConcurrentHashMap<String, CustomPermissionInfo>()

    fun getCustomPermission(name: String) = customPermissions[name]

    fun getCustomPermissions() = customPermissions.values.toList()

    fun registerCustomPermission(info: CustomPermissionInfo) {
        customPermissions[info.name] = info
    }
}