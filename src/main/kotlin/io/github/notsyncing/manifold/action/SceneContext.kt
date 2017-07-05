package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.authenticate.AggregatedPermissions

class SceneContext {
    var scene: ManifoldScene<*>? = null
    var sessionIdentifier: String? = null

    var transaction: ManifoldTransaction<*>? = null
    var autoCommit = true
    var transactionRefCount = 0

    var permissions: AggregatedPermissions? = null

    var additionalData = mutableMapOf<String, Any?>()
}