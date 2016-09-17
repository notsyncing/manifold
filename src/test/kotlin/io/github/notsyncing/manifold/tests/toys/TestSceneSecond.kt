package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestSceneSecond : ManifoldScene<String> {
    companion object {
        var recvEvent = CompletableFuture<ManifoldEvent>()

        const val EventNodeId = "test.scene.second"
        const val EventNodeGroup = "test.scene.group.second"
    }

    constructor() : super(eventNodeId = EventNodeId, eventNodeGroups = arrayOf(EventNodeGroup))

    constructor(event: ManifoldEvent) : super(event)

    override fun init() {
        awakeOnEvent(TestEvent.TestB)
    }

    override fun stage(): CompletableFuture<String> {
        TestSceneSecond.recvEvent.complete(event)

        return CompletableFuture.completedFuture(event!!.data)
    }
}