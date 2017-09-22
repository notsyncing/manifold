package io.github.notsyncing.manifold.eventbus

import io.github.notsyncing.manifold.eventbus.event.*
import io.github.notsyncing.manifold.eventbus.exceptions.NodeGroupNotFoundException
import io.github.notsyncing.manifold.eventbus.exceptions.NodeNotFoundException
import io.github.notsyncing.manifold.eventbus.transports.LocalTransport
import io.github.notsyncing.manifold.eventbus.transports.TransportDescriptor
import io.github.notsyncing.manifold.eventbus.workers.EventBusWorker
import io.github.notsyncing.manifold.utils.FutureUtils
import java.util.concurrent.*
import java.util.logging.Logger

object ManifoldEventBus {
    var listenPort = 8500
    var beaconInterval = 10000L
    var debug = false

    private val workers = ConcurrentHashMap<Class<TransportDescriptor>, EventBusWorker>()
    private val nodes = ConcurrentHashMap<String, ManifoldEventNode>()
    private val groupNodes = ConcurrentHashMap<String, ManifoldEventNodeGroup>()

    private lateinit var beaconScheduler: ScheduledThreadPoolExecutor

    private val beaconTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

    private val logger = Logger.getLogger(javaClass.simpleName)

    private fun createScheduler() = ScheduledThreadPoolExecutor(1) { r ->
        Thread(r).apply {
            this.isDaemon = true
            this.name = "manifold-eventbus-beacon-scheduler"
        }
    }.apply {
        this.removeOnCancelPolicy = true
    }

    fun debug(msg: String) {
        if (debug) {
            logger.info(msg)
        }
    }

    fun <T: TransportDescriptor> registerWorker(worker: EventBusWorker, forTransport: Class<T>) {
        workers[forTransport as Class<TransportDescriptor>] = worker
    }

    fun init(listenPort: Int = 8500, beaconInterval: Long = 10000L) {
        this.listenPort = listenPort
        this.beaconInterval = beaconInterval

        beaconScheduler = createScheduler()

        debug("Manifold event bus initialized")
    }

    fun stop(): CompletableFuture<Void> {
        nodes.clear()
        groupNodes.clear()

        beaconTasks.forEachValue(1) { it.cancel(true) }
        beaconTasks.clear()
        beaconScheduler.shutdownNow()

        return CompletableFuture.allOf(*workers.map { it.value.close() }.toTypedArray())
    }

    fun sendBeacon(node: ManifoldEventNode): CompletableFuture<Boolean> {
        val beaconData = BeaconData(node)
        val beaconEvent = ManifoldEvent(BeaconEvent.Beacon, beaconData)
        beaconEvent.source = node.id
        beaconEvent.type = EventType.Beacon
        beaconEvent.sendType = EventSendType.Broadcast

        debug("Sending beacon from ${node.id}")

        return sendToRemote(beaconEvent)
    }

    private fun addNode(node: ManifoldEventNode) {
        val nodeId = if (node.local) {
            node.id
        } else {
            "${node.id}@${node.transport.source}"
        }

        nodes.put(nodeId, node)

        node.groups.forEach {
            if (!groupNodes.containsKey(it)) {
                groupNodes.put(it, ManifoldEventNodeGroup(it))
            }

            groupNodes[it]!!.add(node)
        }
    }

    private fun removeNode(id: String) {
        val node = nodes.remove(id)

        if (node?.groups != null) {
            node.groups.forEach {
                if (groupNodes.containsKey(it)) {
                    groupNodes[it]!!.remove(node)

                    if (groupNodes[it]!!.count <= 0) {
                        groupNodes.remove(it)
                    }
                }
            }
        }
    }

    fun register(id: String, vararg groups: String): ManifoldEventNode {
        val info = ManifoldEventNode(id, groups as Array<String>, 0, LocalTransport())
        addNode(info)

        debug("Registered local node $id, groups ${groups.joinToString { it }}")

        val task = beaconScheduler.scheduleAtFixedRate({
            if (!nodes.containsKey(id)) {
                return@scheduleAtFixedRate
            }

            val node = nodes[id]!!

            sendBeacon(node).exceptionally {
                it.printStackTrace()
                return@exceptionally null
            }
        }, 0, beaconInterval, TimeUnit.MILLISECONDS)

        if (beaconTasks.containsKey(id)) {
            beaconTasks[id]!!.cancel(true)
        }

        beaconTasks[id] = task

        sendBeacon(info)

        return info
    }

    fun unregister(node: ManifoldEventNode): CompletableFuture<Boolean> {
        nodes.remove(node.id)

        node.groups.forEach {
            if (groupNodes.containsKey(it)) {
                groupNodes[it]!!.remove(node)

                if (groupNodes[it]!!.count <= 0) {
                    groupNodes.remove(it)
                }
            }
        }

        if (beaconTasks.containsKey(node.id)) {
            beaconTasks[node.id]!!.cancel(true)
            beaconTasks.remove(node.id)
            beaconScheduler.purge()
        }

        debug("Unregistered local node ${node.id}")

        val beaconData = BeaconData(node)
        val beaconEvent = ManifoldEvent(BeaconEvent.Exit, beaconData)
        beaconEvent.source = node.id
        beaconEvent.type = EventType.Beacon
        beaconEvent.sendType = EventSendType.Broadcast

        debug("Sending exit beacon from ${node.id}")

        return sendToRemote(beaconEvent)
    }

    fun send(node: ManifoldEventNode, targetId: String, event: ManifoldEvent) {
        event.source = node.id
        event.sendType = EventSendType.Unicast
        event.target = targetId

        debug("Sending event $event to $targetId, counter ${event.counter}")

        val targetNode = nodes[targetId]

        if (targetNode == null) {
            throw NodeNotFoundException("Node $targetId not found!")
        }

        targetNode.receive(event)
    }

    fun sendToAnyInGroup(node: ManifoldEventNode, targetGroup: String, event: ManifoldEvent) {
        event.source = node.id
        event.target = targetGroup
        event.sendType = EventSendType.GroupUnicast

        debug("Sending event $event to any node in group $targetGroup, counter ${event.counter}")

        val targetGroupNodes = groupNodes[targetGroup]

        if ((targetGroupNodes == null) || (targetGroupNodes.count <= 0)) {
            throw NodeGroupNotFoundException("Node group $targetGroup not found or has no child node!")
        }

        targetGroupNodes.receive(event)
    }

    fun broadcast(node: ManifoldEventNode, event: ManifoldEvent) {
        event.source = node.id
        event.sendType = EventSendType.Broadcast

        debug("Broadcasting event $event")

        sendToRemote(event)

        nodes.values.forEach { it.receive(event) }
    }

    fun broadcastToGroup(node: ManifoldEventNode, group: String, event: ManifoldEvent) {
        event.source = node.id
        event.sendType = EventSendType.Groupcast
        event.target = group

        debug("Broadcasting event $event to group $group")

        sendToRemote(event)

        groupNodes[group]?.receive(event)
    }

    fun broadcastToAnyInGroups(node: ManifoldEventNode, event: ManifoldEvent) {
        event.source = node.id
        event.sendType = EventSendType.MultiGroupUnicast

        debug("Broadcasting event $event to any node in all groups")

        sendToRemote(event)

        groupNodes.values.forEach { it.receive(event) }
    }

    fun sendToRemote(event: ManifoldEvent, targetNode: ManifoldEventNode? = null): CompletableFuture<Boolean> {
        if (targetNode == null) {
            return CompletableFuture.allOf(*workers.map { it.value.send(event, null) }.toTypedArray()).thenApply { true }
        }

        if (targetNode.transport is LocalTransport) {
            return FutureUtils.failed(RuntimeException("Local transport cannot be sent to remote node $targetNode, event $event"))
        }

        val worker = workers[targetNode.transport::class.java]

        if (worker == null) {
            return FutureUtils.failed(RuntimeException("Unsupported transport (${targetNode.transport}) in target node $targetNode"))
        }

        return worker.send(event, targetNode)
    }

    fun receivedRemoteEvent(transDesc: TransportDescriptor, event: ManifoldEvent) {
        if (nodes[event.source]?.local == true) {
            return
        }

        debug("Received remote event $event")

        event.source = event.source + "@" + transDesc.source

        if (event.type == EventType.Beacon) {
            val beaconEvent = event
            val beaconData = beaconEvent.getData(BeaconData::class.java)
            val beaconDataSourceNodeId = beaconData.id + "@" + transDesc.source

            if (beaconEvent.event == BeaconEvent.Beacon) {
                val node: ManifoldEventNode

                if (nodes.containsKey(beaconDataSourceNodeId)) {
                    node = nodes[beaconDataSourceNodeId]!!

                    if (node.transport is LocalTransport) {
                        return
                    }

                    node.groups = beaconData.groups
                    node.load = beaconData.load
                    node.transport = transDesc
                } else {
                    node = ManifoldEventNode(beaconDataSourceNodeId, beaconData.groups, beaconData.load, transDesc)
                    addNode(node)
                }
            } else if (beaconEvent.event == BeaconEvent.Exit) {
                removeNode(beaconDataSourceNodeId)
            } else {
                throw RuntimeException("Unsupported beacon event: " + event.event)
            }
        } else if (event.type == EventType.Data) {
            if (event.sendType == EventSendType.Unicast) {
                nodes[event.source]?.receive(event)
            } else if ((event.sendType == EventSendType.GroupUnicast) || (event.sendType == EventSendType.Groupcast)) {
                groupNodes[event.source]?.receive(event)
            } else if (event.sendType == EventSendType.Broadcast) {
                nodes.values.forEach { it.receive(event) }
            } else if (event.sendType == EventSendType.MultiGroupUnicast) {
                groupNodes.values.forEach { it.receive(event) }
            } else {
                throw RuntimeException("Unsupported event send type: " + event.sendType)
            }
        } else {
            throw RuntimeException("Unsupported event type: " + event.type)
        }
    }
}