package io.github.notsyncing.manifold.spec.database

import io.vertx.ext.sql.ResultSet

class DatabaseResult(private val result: ResultSet) {
    class DatabaseResultStore(private val result: ResultSet,
                              private val columnName: String) {
        infix fun into(variable: Ref<*>) {
            (variable as Ref<Any?>).value = result.rows[0].getValue(columnName)
        }
    }

    infix fun store(returnedColumnName: String): DatabaseResultStore {
        return DatabaseResultStore(result, returnedColumnName)
    }
}