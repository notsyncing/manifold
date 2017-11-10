package io.github.notsyncing.manifold.spec.database

import io.github.notsyncing.lightfur.DataSession
import io.github.notsyncing.lightfur.integration.jdbc.JdbcDataSession

val database = DatabaseAccessor()

class DatabaseAccessor {
    init {
        DataSession.setCreator { JdbcDataSession() as DataSession<Any, Any, Any> }
    }

    infix fun execute(sql: String): DatabaseResult {
        val db: JdbcDataSession = DataSession.start()

        try {
            val r = DatabaseResult(db.queryJson(sql).get())
            db.end().get()
            return r
        } catch (e: Exception) {
            db.end().get()
            throw e
        }
    }

    infix fun exists(sql: String): Boolean {
        val db: JdbcDataSession = DataSession.start()

        try {
            val r = db.queryJson(sql).get()
            db.end().get()
            return r.isNotEmpty()
        } catch (e: Exception) {
            db.end().get()
            throw e
        }
    }

    infix fun notExists(sql: String): Boolean {
        return !exists(sql)
    }
}