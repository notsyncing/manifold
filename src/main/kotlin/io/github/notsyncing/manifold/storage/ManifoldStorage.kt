package io.github.notsyncing.manifold.storage

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldTransaction
import io.github.notsyncing.manifold.action.SceneContext
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future

abstract class ManifoldStorage<T> {
    private var ownTrans: ManifoldTransaction<T>? = null

    var sceneContext: SceneContext? = null

    protected val transaction: ManifoldTransaction<T>?
        get() = sceneContext?.transaction as ManifoldTransaction<T>?

    protected val db: T
        get() {
            if (transaction != null) {
                return transaction!!.body
            } else {
                if (ownTrans == null) {
                    ownTrans = Manifold.transactionProvider
                            ?.get(Exception("Created by $this at here")) as ManifoldTransaction<T>?
                }

                return ownTrans!!.body
            }
        }

    fun destroy() = future {
        if (ownTrans != null) {
            ownTrans!!.end().await()
        }
    }
}