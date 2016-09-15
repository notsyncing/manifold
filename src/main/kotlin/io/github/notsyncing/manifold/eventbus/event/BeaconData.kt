package io.github.notsyncing.manifold.eventbus.event

import io.github.notsyncing.manifold.eventbus.ManifoldEventNode

class BeaconData {
    var id: String
    var groups: Array<String> = emptyArray()
    var load: Int

    constructor(node: ManifoldEventNode) {
        this.id = node.id
        this.groups = node.groups
        this.load = node.load
    }
}
