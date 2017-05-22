package io.github.notsyncing.manifold.suspendable

class WaitActionResult {
    var executed = false
    var result: Any? = null
    var exception: Throwable? = null
}