package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

abstract class ManifoldScene<R>() {
    protected var event: ManifoldEvent<*>? = null

    companion object {
        private val eventNodes = ConcurrentHashMap<Class<ManifoldScene<*>>, ArrayList<ManifoldEventNode>>()
    }

    constructor(event: ManifoldEvent<*>) : this() {
        this.event = event
    }

    protected fun attachEventNode(node: ManifoldEventNode) {
        val c = this.javaClass as Class<ManifoldScene<*>>

        if (!eventNodes.containsKey(c)) {
            eventNodes.put(c, ArrayList())
        }

        eventNodes[c]!!.add(node)
    }

    protected fun awakeOnEvent(node: ManifoldEventNode, eventType: Enum<*>) {
        node.on(eventType) {
            val constructor = this.javaClass.getConstructor(ManifoldEvent::class.java)
            constructor.isAccessible = true

            val scene = constructor.newInstance(it)
            scene.stage()
        }
    }

    abstract fun init()

    abstract fun stage(): CompletableFuture<R>
}