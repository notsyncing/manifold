package io.github.notsyncing.manifold.spec.models

class ParameterInfo(val name: String, val fieldName: String) {
    var nullable = false

    infix fun can(v: Any?): ParameterInfo {
        nullable = v == null

        return this
    }
}