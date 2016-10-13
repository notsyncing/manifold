package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestSceneSimple : ManifoldScene<String> {
    constructor() : super(enableEventNode = false)

    constructor(event: ManifoldEvent) : super(event)

    override fun stage(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("Hello!")
    }
}