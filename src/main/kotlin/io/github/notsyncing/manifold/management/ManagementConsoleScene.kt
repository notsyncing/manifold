package io.github.notsyncing.manifold.management

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.action.describe.DataPolicy
import io.github.notsyncing.manifold.authenticate.SpecialAuth
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.manifold.feature.InternalFeatureGroups
import io.github.notsyncing.manifold.feature.ManagementFeatures
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture

@SceneMetadata("manifold.management.console", DataPolicy.Modify)
@Feature(ManagementFeatures.Console, groups = arrayOf(InternalFeatureGroups.Management),
        defaultSpecialAuths = arrayOf(SpecialAuth.SuperUserOnly))
class ManagementConsoleScene(private val args: Array<String>) : ManifoldScene<String>() {
    constructor() : this(emptyArray())

    override fun stage() = future {
        if (args.isEmpty()) {
            return@future "ERR_NO_COMMAND"
        }

        val cmd = args[0]

        if (cmd.isEmpty()) {
            return@future "ERR_NO_COMMAND"
        }

        val method = Manifold.managementCommands.getCommand(cmd)

        if (method == null) {
            return@future "ERR_UNSUPPORTED_COMMAND"
        }

        val params = args.sliceArray(1 until args.size)

        if (params.size != method.parameterCount) {
            return@future "ERR_WRONG_PARAMETER_COUNT"
        }

        val r = method.invoke(null, *params)

        if (r is CompletableFuture<*>) {
            return@future r.await().toString()
        } else {
            return@future r.toString()
        }
    }
}