package io.github.notsyncing.manifold.authenticate

class AuthRole(val roleId: String = "",
               val groups: Array<AuthGroup> = emptyArray(),
               permissions: Array<Permission>) : AuthItem(permissions) {
    constructor(roleId: Long = 0, groups: Array<AuthGroup> = emptyArray(), permissions: Array<Permission>)
            : this(if (roleId == 0L) "" else roleId.toString(), groups, permissions)
}