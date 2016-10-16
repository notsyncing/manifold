package io.github.notsyncing.manifold.storage

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldTransaction
import io.github.notsyncing.manifold.action.SceneContext

abstract class ManifoldStorage<T> {
    private var ownDb: T? = null

    var sceneContext: SceneContext? = null

    protected val transaction: ManifoldTransaction<T>?
        get() = sceneContext?.transaction as ManifoldTransaction<T>?

    protected val db: T
        get() {
            if (transaction != null) {
                return transaction!!.body
            } else {
                if (ownDb == null) {
                    ownDb = Manifold.transactionProvider?.get()?.body!! as T
                }

                return ownDb!!
            }
        }
}