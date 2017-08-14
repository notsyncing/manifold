package io.github.notsyncing.manifold.eventbus.workers

import io.github.notsyncing.manifold.eventbus.transports.TransportDescriptor

open class NetTransport(val host: String?, val port: Int) : TransportDescriptor() {
    override val source: String
        get() = "$host:$port"
}