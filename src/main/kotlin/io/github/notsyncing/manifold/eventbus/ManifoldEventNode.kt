package io.github.notsyncing.manifold.eventbus

import io.github.notsyncing.manifold.eventbus.event.EventSendType
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.*
import java.util.concurrent.*

class ManifoldEventNode(var id: String,
                        var groups: Array<String>?,
                        var load: Int,
                        var address: String?) {
    companion object {
        private val replyCallbackTimeout = Executors.newScheduledThreadPool(1)
    }

    typealias ManifoldEventHandler<TS> = (ManifoldEvent<TS>) -> Unit

    var local: Boolean = address == null
        get() = address == null

    val handlers = ConcurrentHashMap<Any, ArrayList<ManifoldEventHandler<*>>>()
    val replyCallbacks = ConcurrentHashMap<Long, CompletableFuture<ManifoldEvent<*>>>()
    val replyCallbackTimeoutSchedulers = ConcurrentHashMap<Long, ScheduledFuture<*>>()

    private fun processEvent(event: ManifoldEvent<*>) {
        if (event.replyToCounter > 0) {
            val f = replyCallbacks.remove(event.replyToCounter)
            replyCallbackTimeoutSchedulers.remove(event.replyToCounter)
            f!!.complete(event)
            return
        }

        if (!handlers.containsKey(event.event)) {
            return
        }

        handlers[event.event]!!.forEach {
            it.invoke(event)
        }
    }

    fun increaseLoad() {
        load++
    }

    fun decreaseLoad() {
        load--

        if (load < 0) {
            load = 0
        }
    }

    fun unregister() = ManifoldEventBus.unregister(this)

    fun <TS: Enum<TS>> on(event: TS, handler: ManifoldEventHandler<TS>) {
        if (!handlers.containsKey(event)) {
            handlers.put(event, ArrayList())
        }

        handlers[event]!!.add(handler as ManifoldEventHandler<*>)
    }

    fun send(targetId: String, event: ManifoldEvent<*>) = ManifoldEventBus.send(this, targetId, event)

    fun <T: Enum<T>> sendAndWaitForReply(targetId: String, event: ManifoldEvent<*>, timeout: Long = 10000): CompletableFuture<ManifoldEvent<T>?> {
        val f = CompletableFuture<ManifoldEvent<T>?>()
        replyCallbacks.put(event.counter, f as CompletableFuture<ManifoldEvent<*>>)

        send(targetId, event)

        if (timeout > 0) {
            val s = replyCallbackTimeout.schedule({
                if ((!f.isDone) && (!f.isCancelled) && (!f.isCompletedExceptionally)) {
                    f.completeExceptionally(TimeoutException("Timeout ($timeout ms) while waiting for reply: target $targetId, source event $event"))
                }

                replyCallbackTimeoutSchedulers.remove(event.replyToCounter)
            }, timeout, TimeUnit.MILLISECONDS)

            replyCallbackTimeoutSchedulers.put(event.counter, s)
        }

        return f
    }

    fun sendToAnyInGroup(targetGroup: String, event: ManifoldEvent<*>)
            = ManifoldEventBus.sendToAnyInGroup(this, targetGroup, event)

    fun <T: Enum<T>> sendToAnyInGroupAndWaitForReply(targetGroup: String, event: ManifoldEvent<*>, timeout: Long = 10000): CompletableFuture<ManifoldEvent<T>?> {
        val f = CompletableFuture<ManifoldEvent<T>?>()
        replyCallbacks.put(event.counter, f as CompletableFuture<ManifoldEvent<*>>)

        sendToAnyInGroup(targetGroup, event)

        if (timeout > 0) {
            replyCallbackTimeout.schedule({
                if ((!f.isDone) && (!f.isCancelled) && (!f.isCompletedExceptionally)) {
                    f.completeExceptionally(TimeoutException("Timeout ($timeout ms) while waiting for reply: target group $targetGroup, source event $event"))
                }
            }, timeout, TimeUnit.MILLISECONDS)
        }

        return f
    }

    fun broadcast(event: ManifoldEvent<*>) = ManifoldEventBus.broadcast(this, event)

    fun broadcastToGroup(group: String, event: ManifoldEvent<*>) = ManifoldEventBus.broadcastToGroup(this, group, event)

    fun broadcastToAnyInGroups(event: ManifoldEvent<*>) = ManifoldEventBus.broadcastToAnyInGroups(this, event)

    fun <T: Enum<T>> reply(sourceEvent: ManifoldEvent<*>, newEvent: ManifoldEvent<T>?) {
        val event = newEvent ?: ManifoldEvent<T>()

        event.replyToCounter = sourceEvent.counter
        event.source = this.id
        event.target = sourceEvent.source

        send(event.target, event)
    }

    fun receive(event: ManifoldEvent<*>) {
        if (local) {
            processEvent(event)
        } else {
            if ((event.sendType != EventSendType.Broadcast) && (event.sendType != EventSendType.MultiGroupUnicast)
                    && (event.sendType != EventSendType.Groupcast)) {
                ManifoldEventBus.sendToRemote(event, this)
            }
        }
    }
}