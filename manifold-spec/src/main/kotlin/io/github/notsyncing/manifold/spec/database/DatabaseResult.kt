package io.github.notsyncing.manifold.spec.database

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.spec.models.Ref

class DatabaseResult(private val result: List<JSONObject>) {
    class DatabaseResultStore(private val result: List<JSONObject>,
                              private val columnName: String) {
        infix fun into(variable: Ref<*>) {
            if (result.isEmpty()) {
                throw RuntimeException("No data returned!")
            }

            (variable as Ref<Any?>).value = result[0].get(columnName)
        }
    }

    infix fun store(returnedColumnName: String): DatabaseResultStore {
        return DatabaseResultStore(result, returnedColumnName)
    }
}