package io.github.notsyncing.manifold.authenticate

class AuthGroup(val groupId: Long = 0,
                val parentGroup: AuthGroup? = null,
                permissions: Array<Permission>) : AuthItem(permissions) {
}