package io.github.notsyncing.manifold.authenticate

open class AuthRole(val roleId: String = "",
                    val sessionId: String = "",
                    val groups: Array<AuthGroup> = emptyArray(),
                    permissions: Array<Permission>) : AuthItem(permissions) {
    constructor(roleId: Long = 0, sessionId: String = "", groups: Array<AuthGroup> = emptyArray(), permissions: Array<Permission>)
            : this(if (roleId == 0L) "" else roleId.toString(), sessionId, groups, permissions)

    fun isSuperUser() = this == SpecialRole.SuperUser
}