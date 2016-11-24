package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestSceneSimple : ManifoldScene<String> {
    private var exception: Boolean = false

    constructor() : super(enableEventNode = false)

    constructor(exception: Boolean = false) : this() {
        this.exception = exception
    }

    constructor(event: ManifoldEvent) : super(event)

    override fun stage(): CompletableFuture<String> {
        if (exception) {
            throw RuntimeException()
        }

        return CompletableFuture.completedFuture("Hello!")
    }
}