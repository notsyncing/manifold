package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestSceneFirst2 : ManifoldScene<String> {
    companion object {
        const val EventNodeId = "test.scene.first"
        const val EventNodeGroup = "test.scene.group.first"
    }

    constructor() : super(eventNodeId = EventNodeId, eventNodeGroups = arrayOf(EventNodeGroup))

    constructor(event: ManifoldEvent) : super(event)

    override fun stage(): CompletableFuture<String> {
        transitionTo<Any>(TestSceneSecond.EventNodeGroup, arrayOf(2, "Test data"))

        return CompletableFuture.completedFuture("Hello2!")
    }
}