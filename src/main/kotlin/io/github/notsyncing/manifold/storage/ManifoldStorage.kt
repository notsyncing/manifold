package io.github.notsyncing.manifold.storage

import io.github.notsyncing.manifold.action.ManifoldTransaction
import io.github.notsyncing.manifold.action.SceneContext

abstract class ManifoldStorage<T> {
    lateinit var sceneContext: SceneContext

    protected val transaction: ManifoldTransaction<T>
        get() = sceneContext.transaction!! as ManifoldTransaction<T>

    protected val db: T
        get() = transaction.body
}