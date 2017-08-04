package io.github.notsyncing.manifold.story.drama

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.action.describe.DataPolicy
import io.github.notsyncing.manifold.authenticate.NoPermissionException
import io.github.notsyncing.manifold.authenticate.PermissionState
import io.github.notsyncing.manifold.utils.FutureUtils
import java.util.concurrent.CompletableFuture

@SceneMetadata("manifold.drama.entry", DataPolicy.Modify)
class DramaScene(private val action: String,
                 private val parameters: JSONObject): ManifoldScene<Any?>() {
    constructor() : this("", JSONObject())

    override fun init() {
        super.init()

        DramaManager.loadAllDramas()
    }

    override fun stage(): CompletableFuture<Any?> {
        val actionInfo = DramaManager.getAction(action)

        if (actionInfo == null) {
            return FutureUtils.failed(ClassNotFoundException("No such action $action found in dramas"))
        }

        val permission = context.permissions?.get(actionInfo.permissionName, actionInfo.permissionType)

        if (permission?.state != PermissionState.Allowed) {
            return FutureUtils.failed(NoPermissionException("Drama action $action needs " +
                    "${actionInfo.permissionName} ${actionInfo.permissionType} to perform, but " +
                    "got state ${permission?.state}"))
        }

        return DramaManager.perform(this, actionInfo, parameters,
                JSON.toJSON(permission.additionalData) as JSONObject?)
    }
}