package io.github.notsyncing.manifold.eventbus.workers

import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture

abstract class EventBusWorker {
    abstract fun send(event: ManifoldEvent, targetNode: ManifoldEventNode? = null): CompletableFuture<Boolean>

    abstract fun close(): CompletableFuture<Void>
}