package io.github.notsyncing.manifold.authenticate

import java.security.InvalidParameterException
import java.util.concurrent.ConcurrentHashMap

class PermissionManager {
    private val customPermissions = ConcurrentHashMap<Int, CustomPermissionInfo>()
    private val customPermissionNameIndex = ConcurrentHashMap<String, CustomPermissionInfo>()

    fun getCustomPermission(id: Int) = customPermissions[id]

    fun getCustomPermission(name: String) = customPermissionNameIndex[name]

    fun getCustomPermissions() = customPermissions.values.toList()

    fun registerCustomPermission(info: CustomPermissionInfo) {
        if (info.id >= 0) {
            throw InvalidParameterException("Custom permission id must be smaller than 0, current ${info.id} in " +
                    "permission ${info.name}")
        }

        customPermissions[info.id] = info
        customPermissionNameIndex[info.name] = info
    }

    fun getPermissionId(name: String): Int {
        val customId = getCustomPermission(name)?.id

        if (customId != null) {
            return customId
        }

        for (ec in SceneAuthenticator.authModuleEnumClass.enumConstants) {
            if (ec.name == name) {
                return ec.ordinal
            }
        }

        throw InvalidParameterException("Unknown permission $name")
    }
}