package io.github.notsyncing.manifold.authenticate

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.interceptors.SceneInterceptor
import io.github.notsyncing.manifold.action.interceptors.SceneInterceptorContext
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import java.util.*
import java.util.concurrent.CompletableFuture

abstract class SceneAuthenticator : SceneInterceptor() {
    companion object {
        var authOrder = arrayOf(AuthOrder.Role, AuthOrder.UpperGroup)
        var denyUndefinedPermissions = true

        lateinit var authModuleEnumClass: Class<Enum<*>>
        lateinit var authTypeEnumClass: Class<Enum<*>>

        fun reset() {
            authOrder = arrayOf(AuthOrder.Role, AuthOrder.UpperGroup)
            denyUndefinedPermissions = true
        }

        fun aggregatePermissions(role: AuthRole): AggregatedPermissions {
            if (role.groups.isEmpty()) {
                return AggregatedPermissions(role.permissions.toList())
            }

            val aggPerms = ArrayList<Permission>()

            fun addPermissions(perms: Array<Permission>, to: ArrayList<Permission>, mergingRoots: Boolean = false) {
                for (p in perms) {
                    val pp = to.firstOrNull { (it.module == p.module) && (it.type == p.type) }

                    if (pp == null) {
                        to.add(p)
                    } else if ((mergingRoots) && (pp.state == PermissionState.Forbidden) && (p.state == PermissionState.Allowed)) {
                        to[to.indexOf(pp)] = p
                    }
                }
            }

            for (g in role.groups) {
                val permsPerBranch = ArrayList<Permission>()

                for (o in authOrder) {
                    if (o == AuthOrder.Role) {
                        addPermissions(role.permissions, permsPerBranch)
                    } else if (o == AuthOrder.LowerGroup) {
                        var item: AuthGroup? = g

                        while (item != null) {
                            addPermissions(item.permissions, permsPerBranch)
                            item = item.parentGroup
                        }
                    } else if (o == AuthOrder.UpperGroup) {
                        val l = ArrayList<AuthGroup>()
                        var item: AuthGroup? = g

                        while (item != null) {
                            l.add(item)
                            item = item.parentGroup
                        }

                        l.reverse()

                        for (g2 in l) {
                            addPermissions(g2.permissions, permsPerBranch)
                        }
                    }
                }

                addPermissions(permsPerBranch.toTypedArray(), aggPerms, true)
            }

            return AggregatedPermissions(aggPerms)
        }
    }

    private lateinit var currentRole: AuthRole
    private val authInfoProvider: AuthenticateInformationProvider? = Manifold.authInfoProvider

    protected fun SceneInterceptorContext.pass(): CompletableFuture<Unit> {
        return CompletableFuture.completedFuture(Unit)
    }

    protected fun SceneInterceptorContext.deny(): CompletableFuture<Unit> {
        this.stop(NoPermissionException("${this@SceneAuthenticator} denied access", this))
        return CompletableFuture.completedFuture(Unit)
    }

    abstract fun authenticate(context: SceneInterceptorContext, role: AuthRole): CompletableFuture<Unit>

    override fun before(context: SceneInterceptorContext) = async {
        if (authInfoProvider == null) {
            return@async
        }

        val id = context.sceneContext.sessionIdentifier
        var role: AuthRole?

        if (id == null) {
            role = AuthRole(permissions = emptyArray())
        } else {
            role = authInfoProvider.getRole(id).await()
        }

        if (role == null) {
            role = AuthRole(permissions = emptyArray())
        }

        if (role.roleId == SpecialRole.SuperUser) {
            return@async context.pass().await()
        }

        currentRole = role

        return@async authenticate(context, currentRole).await()
    }

    protected fun SceneInterceptorContext.aggregatePermissions(): AggregatedPermissions {
        val a = Companion.aggregatePermissions(currentRole)
        sceneContext.permissions = a

        return a
    }

    private fun getPermissionFromRole(module: Enum<*>, type: Enum<*>): Permission {
        for (o in authOrder) {
            if (o == AuthOrder.Role) {
                for (p in currentRole.permissions) {
                    if ((p.module == module) && (p.type == type)) {
                        return p
                    }
                }
            } else if (o == AuthOrder.LowerGroup) {
                var currPerm: Permission? = null

                for (g in currentRole.groups) {
                    var item: AuthGroup? = g

                    while (item != null) {
                        var found = false

                        for (p in item.permissions) {
                            if ((p.module == module) && (p.type == type)) {
                                if (currPerm == null) {
                                    currPerm = p
                                } else if ((currPerm.state == PermissionState.Forbidden) && (p.state == PermissionState.Allowed)) {
                                    currPerm = p
                                }

                                found = true
                                break
                            }
                        }

                        if (found) {
                            break
                        }

                        item = item.parentGroup
                    }
                }

                if (currPerm != null) {
                    return currPerm
                }
            } else if (o == AuthOrder.UpperGroup) {
                var currPerm: Permission? = null

                for (g in currentRole.groups) {
                    val list = ArrayList<AuthGroup>()
                    var item: AuthGroup? = g

                    while (item != null) {
                        list.add(item)
                        item = item.parentGroup
                    }

                    list.reverse()

                    for (g2 in list) {
                        var found = false

                        for (p in g2.permissions) {
                            if ((p.module == module) && (p.type == type)) {
                                if (currPerm == null) {
                                    currPerm = p
                                } else if ((currPerm.state == PermissionState.Forbidden) && (p.state == PermissionState.Allowed)) {
                                    currPerm = p
                                }

                                found = true
                                break
                            }
                        }

                        if (found) {
                            break
                        }
                    }
                }

                if (currPerm != null) {
                    return currPerm
                }
            }
        }

        return Permission(module, type, PermissionState.Undefined)
    }

    protected fun SceneInterceptorContext.hasPermission(module: Enum<*>, type: Enum<*>): Boolean {
        val p: Permission

        if (this.sceneContext.permissions != null) {
            p = this.sceneContext.permissions!!.get(module, type)
        } else {
            p = getPermissionFromRole(module, type)
        }

        if (p.state == PermissionState.Forbidden) {
            return false
        } else if ((p.state == PermissionState.Undefined) && (denyUndefinedPermissions)) {
            return false
        } else {
            return true
        }
    }

    protected fun SceneInterceptorContext.getPermission(module: Enum<*>, type: Enum<*>): Permission {
        if (this.sceneContext.permissions != null) {
            return this.sceneContext.permissions!!.get(module, type)
        } else {
            return getPermissionFromRole(module, type)
        }
    }

    protected fun SceneInterceptorContext.checkPermission(module: Enum<*>, type: Enum<*>): CompletableFuture<Unit> {
        return if (hasPermission(module, type)) pass() else deny()
    }

    override fun destroy(context: SceneInterceptorContext) = async<Unit> {
        if (authInfoProvider != null) {
            authInfoProvider.destroy().await()
        }

        super.destroy(context).await()
    }
}