package io.github.notsyncing.manifold.spec.models

class ReturnInfo {
    var returnType: Class<*>? = null

    operator fun invoke(type: Class<*>): ReturnInfo {
        this.returnType = type
        return this
    }
}