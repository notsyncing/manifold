package io.github.notsyncing.manifold.spec.database

import io.github.notsyncing.lightfur.DataSession
import io.github.notsyncing.lightfur.entity.EntityDataMapper

val database = DatabaseAccessor()

class DatabaseAccessor {
    infix fun execute(sql: String): DatabaseResult {
        val db = DataSession(EntityDataMapper())

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
        val db = DataSession(EntityDataMapper())

        try {
            val r = db.query(sql).get()
            db.end().get()
            return r.numRows > 0
        } catch (e: Exception) {
            db.end().get()
            throw e
        }
    }
}