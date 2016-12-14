package io.github.notsyncing.manifold.spec.database

import io.github.notsyncing.lightfur.DataSession
import io.github.notsyncing.lightfur.entity.EntityDataMapper
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

val database = DatabaseAccessor()

class DatabaseAccessor {
    infix fun execute(sql: String): DatabaseResult {
        val db = DataSession(EntityDataMapper())
        val r = DatabaseResult(db.executeWithReturning(sql).get())

        db.end().get()

        return r
    }

    infix fun exists(sql: String): Boolean {
        val f = CompletableFuture<Boolean>()

        thread {
            val db = DataSession(EntityDataMapper())

            db.query(sql).thenAccept {
                db.end()
                f.complete(it.numRows > 0)
            }.exceptionally {
                db.end()
                f.completeExceptionally(it)
                null
            }
        }

        return f.get()
    }
}