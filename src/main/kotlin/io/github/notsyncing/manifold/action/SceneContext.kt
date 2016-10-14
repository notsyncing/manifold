package io.github.notsyncing.manifold.action

class SceneContext {
    var transaction: ManifoldTransaction<*>? = null
    var autoCommit = true
}