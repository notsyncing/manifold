package io.github.notsyncing.manifold.eventbus.exceptions

class NodeNotFoundException : Exception {
    constructor() {
    }

    constructor(message: String) : super(message) {
    }
}
