package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestScene : ManifoldScene<String> {
    companion object {
        var initExecuted = false
        var recvEvent = CompletableFuture<ManifoldEvent>()
    }

    constructor() : super()

    constructor(event: ManifoldEvent) : super(event)

    override fun init() {
        val node = ManifoldEventBus.register("test.scene")
        attachEventNode(node)
        awakeOnEvent(node, TestEvent.TestA)

        initExecuted = true
    }

    override fun stage(): CompletableFuture<String> {
        recvEvent.complete(event)

        return CompletableFuture.completedFuture("Hello!")
    }
}