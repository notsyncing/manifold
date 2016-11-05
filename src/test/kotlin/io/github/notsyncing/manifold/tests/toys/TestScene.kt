package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.di.AutoProvide
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

class TestScene : ManifoldScene<String> {
    companion object {
        var initExecuted = false
        var recvEvent = CompletableFuture<ManifoldEvent>()

        const val EventNodeId = "test.scene"
        const val EventNodeGroup = "test.scene.group1"
    }

    @AutoProvide
    lateinit var testManager: TestManager

    @AutoProvide
    lateinit var testManager2: TestManager

    constructor() : super(enableEventNode = true, eventNodeId = EventNodeId, eventNodeGroups = arrayOf(EventNodeGroup))

    constructor(event: ManifoldEvent) : super(event)

    override fun init() {
        super.init()

        awakeOnEvent(TestEvent.TestA)

        initExecuted = true
    }

    override fun stage(): CompletableFuture<String> {
        recvEvent.complete(event)

        return CompletableFuture.completedFuture("Hello!")
    }
}