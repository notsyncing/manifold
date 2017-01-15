package io.github.notsyncing.manifold.eventbus

import io.github.notsyncing.manifold.eventbus.workers.EventBusNetWorker
import io.github.notsyncing.manifold.eventbus.workers.NetTransport

fun ManifoldEventBus.network() {
    this.registerWorker(EventBusNetWorker(this.listenPort, this.listenPort, this::receivedRemoteEvent), NetTransport::class.java)
}