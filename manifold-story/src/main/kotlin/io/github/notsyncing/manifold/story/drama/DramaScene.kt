package io.github.notsyncing.manifold.story.drama

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.action.describe.DataPolicy
import io.github.notsyncing.manifold.authenticate.*
import io.github.notsyncing.manifold.utils.FutureUtils
import java.lang.UnsupportedOperationException
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import java.util.logging.Logger

@SceneMetadata("manifold.drama.entry", DataPolicy.Modify)
class DramaScene(private val action: String,
                 private val domain: String? = null,
                 private val parameters: JSONObject = JSONObject()): ManifoldScene<Any?>() {
    private val logger = Logger.getLogger(javaClass.simpleName)

    constructor() : this("")

    override fun stage(): CompletableFuture<Any?> {
        val actionInfo = DramaManager.getAction(action, domain)

        if (actionInfo == null) {
            return FutureUtils.failed(ClassNotFoundException("No such action $action found in dramas"))
        }

        if (actionInfo.internal) {
            return FutureUtils.failed(UnsupportedOperationException("The action $action is for internal use!"))
        }

        var permission: Permission? = null

        if ((actionInfo.permissionName != null) && (actionInfo.permissionType != null)) {
            if (context.role == null) {
                return FutureUtils.failed(NoPermissionException("Drama action $action has permission, but no role provided!"))
            }

            if (context.role != SpecialRole.SuperUser) {
                if (context.permissions == null) {
                    context.permissions = SceneAuthenticator.aggregatePermissions(context.role!!)
                }

                permission = context.permissions?.get(actionInfo.permissionName, actionInfo.permissionType)

                if (permission?.state != PermissionState.Allowed) {
                    return FutureUtils.failed(NoPermissionException("Drama action $action needs " +
                            "${actionInfo.permissionName} ${actionInfo.permissionType} to perform, but " +
                            "got state ${permission?.state}"))
                }
            }
        } else if (actionInfo.permissionName == SpecialAuth.LoginOnly) {
            if ((context.role?.roleId == "0") || (context.role?.roleId == null) || (context.role?.roleId == "")) {
                return FutureUtils.failed(NoPermissionException("Drama action $action needs login!"))
            }
        }

        return DramaManager.perform(this, actionInfo, parameters,
                if (permission == null) null else JSON.toJSON(permission.additionalData) as JSONObject?)
    }

    override fun onFailure(exception: Throwable): CompletableFuture<Pair<Boolean, Any?>> {
        if (exception is DramaExecutionException) {
            val o = JSONObject()
                    .fluentPut("error", exception.message)

            logger.log(Level.WARNING, "An exception occured when processing action $action in domain $domain with " +
                    "parameters $parameters: ${exception.message}", exception)

            return CompletableFuture.completedFuture(Pair(true, o))
        } else {
            return super.onFailure(exception)
        }
    }
}