package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ForTransition
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestSceneSecond : ManifoldScene<String> {
    companion object {
        var recvEvent = CompletableFuture<ManifoldEvent>()
        var contEvent = CompletableFuture<Any?>()
        var id: Int = 0
        var name: String = ""
        var sessionId: String = ""

        const val EventNodeId = "test.scene.second"
        const val EventNodeGroup = "test.scene.group.second"

        fun reset() {
            id = 0
            name = ""

            recvEvent = CompletableFuture()
            contEvent = CompletableFuture()
        }
    }

    constructor() : super(enableEventNode = true, eventNodeId = EventNodeId, eventNodeGroups = arrayOf(EventNodeGroup))

    constructor(event: ManifoldEvent) : super(event)

    @ForTransition
    constructor(id: Int, name: String) : this() {
        TestSceneSecond.id = id
        TestSceneSecond.name = name
    }

    override fun init() {
        super.init()

        awakeOnEvent(TestEvent.TestB)
    }

    override fun stage(): CompletableFuture<String> {
        TestSceneSecond.sessionId = m.sessionIdentifier ?: ""

        TestSceneSecond.recvEvent.complete(event)
        TestSceneSecond.contEvent.complete(null)

        return CompletableFuture.completedFuture(event?.data)
    }
}