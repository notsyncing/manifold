package io.github.notsyncing.manifold.spec.database

import io.github.notsyncing.lightfur.DataSession
import io.github.notsyncing.lightfur.integration.vertx.VertxDataSession

val database = DatabaseAccessor()

class DatabaseAccessor {
    init {
        DataSession.setCreator { VertxDataSession() as DataSession<Any, Any, Any> }
    }

    infix fun execute(sql: String): DatabaseResult {
        val db: VertxDataSession = DataSession.start()

        try {
            val r = DatabaseResult(db.executeWithReturning(sql).get())
            db.end().get()
            return r
        } catch (e: Exception) {
            db.end().get()
            throw e
        }
    }

    infix fun exists(sql: String): Boolean {
        val db: VertxDataSession = DataSession.start()

        try {
            val r = db.query(sql).get()
            db.end().get()
            return r.numRows > 0
        } catch (e: Exception) {
            db.end().get()
            throw e
        }
    }

    infix fun notExists(sql: String): Boolean {
        return !exists(sql)
    }
}