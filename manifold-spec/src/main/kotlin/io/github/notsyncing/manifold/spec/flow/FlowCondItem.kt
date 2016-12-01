package io.github.notsyncing.manifold.spec.flow

class FlowCondItem(val cond: String) : FlowItem(cond) {
    var nextTrue: FlowItem?
        get() = next
        set(value) {
            next = value
        }

    var nextFalse: FlowItem? = null
}