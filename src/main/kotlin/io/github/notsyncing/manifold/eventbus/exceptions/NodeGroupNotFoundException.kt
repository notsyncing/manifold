package io.github.notsyncing.manifold.eventbus.exceptions

class NodeGroupNotFoundException : Exception {
    constructor() {
    }

    constructor(message: String) : super(message) {
    }
}
