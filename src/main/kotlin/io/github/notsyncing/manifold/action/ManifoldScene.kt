package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

abstract class ManifoldScene<R>(enableEventNode: Boolean = true, eventNodeId: String = "",
                                eventNodeGroups: Array<String> = emptyArray()) {
    protected var event: ManifoldEvent? = null
    protected var eventNode: ManifoldEventNode? = null
    lateinit var m: ManifoldRunner

    companion object {
        private val eventNodes = ConcurrentHashMap<Class<ManifoldScene<*>>, ManifoldEventNode>()

        fun reset() {
            eventNodes.clear()
        }
    }

    init {
        val c = this.javaClass as Class<ManifoldScene<*>>

        if (enableEventNode) {
            if (eventNodes.containsKey(c)) {
                eventNode = eventNodes[c]
            } else {
                eventNode = ManifoldEventBus.register(eventNodeId, *eventNodeGroups)
                eventNodes[c] = eventNode!!
            }
        }
    }

    constructor(event: ManifoldEvent) : this() {
        this.event = event
    }

    protected fun awakeOnEvent(event: String) {
        eventNode?.on(event) {
            Manifold.run(this.javaClass, it)
        }
    }

    protected fun transitionTo(targetEventGroup: String, event: String, data: Any) {
        eventNode?.sendToAnyInGroup(targetEventGroup, ManifoldEvent(event, data))
    }

    protected fun transitionTo(targetEventGroup: String, event: String, data: Any, waitForResult: Boolean): CompletableFuture<ManifoldEvent?> {
        assert(waitForResult) { "You must specify value TRUE for parameter waitForResult!" }
        return eventNode?.sendToAnyInGroupAndWaitForReply(targetEventGroup, ManifoldEvent(event, data)) ?: CompletableFuture.completedFuture(null as ManifoldEvent?)
    }

    protected fun <A> transitionTo(targetScene: Class<ManifoldScene<A>>, event: String, data: Any): CompletableFuture<A> {
        return Manifold.run(targetScene, ManifoldEvent(event, data))
    }

    open fun init() {

    }

    abstract fun stage(): CompletableFuture<R>
}