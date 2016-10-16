package io.github.notsyncing.manifold.authenticate

class AuthRole(val roleId: Long = 0,
               val groups: Array<AuthGroup> = emptyArray(),
               permissions: Array<Permission>) : AuthItem(permissions) {
}