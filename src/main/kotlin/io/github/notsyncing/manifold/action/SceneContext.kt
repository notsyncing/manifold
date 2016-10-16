package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.authenticate.AggregatedPermissions

class SceneContext {
    var sessionIdentifier: String? = null

    var transaction: ManifoldTransaction<*>? = null
    var autoCommit = true

    var permissions: AggregatedPermissions? = null
}