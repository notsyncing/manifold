package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestSceneFirst : ManifoldScene<String> {
    companion object {
        const val EventNodeId = "test.scene.first"
        const val EventNodeGroup = "test.scene.group.first"
    }

    constructor() : super(enableEventNode = true, eventNodeId = EventNodeId, eventNodeGroups = arrayOf(EventNodeGroup))

    constructor(event: ManifoldEvent) : super(event)

    override fun stage(): CompletableFuture<String> {
        transitionTo(TestSceneSecond.EventNodeGroup, TestEvent.TestB, "Test data")

        return CompletableFuture.completedFuture("Hello2!")
    }
}