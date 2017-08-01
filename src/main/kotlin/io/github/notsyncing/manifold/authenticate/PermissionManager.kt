package io.github.notsyncing.manifold.authenticate

import java.security.InvalidParameterException
import java.util.concurrent.ConcurrentHashMap

class PermissionManager {
    private val customPermissions = ConcurrentHashMap<Int, CustomPermissionInfo>()

    fun getCustomPermission(id: Int) = customPermissions[id]

    fun getCustomPermissions() = customPermissions.values.toList()

    fun registerCustomPermission(info: CustomPermissionInfo) {
        if (info.id >= 0) {
            throw InvalidParameterException("Custom permission id must be smaller than 0, current ${info.id} in " +
                    "permission ${info.name}")
        }

        customPermissions[info.id] = info
    }
}