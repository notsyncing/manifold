package io.github.notsyncing.manifold.action

class SceneContext {
    var sessionIdentifier: String? = null

    var transaction: ManifoldTransaction<*>? = null
    var autoCommit = true
}