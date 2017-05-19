package io.github.notsyncing.manifold.suspendable

class ActionResult {
    var executed = false
    var result: Any? = null
    var exception: Throwable? = null
}