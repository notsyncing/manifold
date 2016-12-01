package io.github.notsyncing.manifold.spec.flow

open class FlowItem(val text: String) {
    var previous: FlowItem? = null
    var next: FlowItem? = null
}