package io.github.notsyncing.manifold.action

import com.alibaba.fastjson.JSON
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.eventbus.ManifoldEventBus
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.InternalEvent
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

                awakeOnEvent(InternalEvent.TransitionToScene)
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

    protected fun transitionTo(targetEventGroup: String, event: String, data: Any, waitForResult: Boolean = false): CompletableFuture<ManifoldEvent?> {
        if (waitForResult) {
            return eventNode?.sendToAnyInGroupAndWaitForReply(targetEventGroup, ManifoldEvent(event, data)) ?: CompletableFuture.completedFuture(null as ManifoldEvent?)
        } else {
            eventNode?.sendToAnyInGroup(targetEventGroup, ManifoldEvent(event, data))
            return CompletableFuture.completedFuture(null)
        }
    }

    protected inline fun <reified A: Any> transitionTo(targetSceneEventGroup: String, parameters: Array<Any>? = null, waitForResult: Boolean = false): CompletableFuture<A?> {
        val d = JSON.toJSONString(parameters)

        if (waitForResult) {
            return eventNode?.sendToAnyInGroupAndWaitForReply(targetSceneEventGroup, ManifoldEvent(InternalEvent.TransitionToScene, d))
                    ?.thenApply { JSON.parseObject(it?.data, A::class.java) } ?: CompletableFuture.completedFuture(null as A)
        } else {
            eventNode?.sendToAnyInGroup(targetSceneEventGroup, ManifoldEvent(InternalEvent.TransitionToScene, d))
            return CompletableFuture.completedFuture(null)
        }
    }

    open fun init() {

    }

    abstract fun stage(): CompletableFuture<R>
}