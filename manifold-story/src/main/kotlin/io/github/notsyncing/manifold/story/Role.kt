package io.github.notsyncing.manifold.story

import io.github.notsyncing.manifold.authenticate.AuthGroup
import io.github.notsyncing.manifold.authenticate.AuthRole
import io.github.notsyncing.manifold.authenticate.Permission

open class Role(roleId: String = "") : AuthRole(roleId, emptyArray<AuthGroup>(), emptyArray<Permission>()) {

}