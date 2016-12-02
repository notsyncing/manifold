package io.github.notsyncing.manifold.spec.models

class ParameterInfo(val name: String, val fieldName: String) {
    var type: Class<*>? = null
    var nullable = false

    infix fun type(t: Class<*>): ParameterInfo {
        type = t
        return this
    }

    infix fun can(v: Any?): ParameterInfo {
        nullable = v == null

        return this
    }
}