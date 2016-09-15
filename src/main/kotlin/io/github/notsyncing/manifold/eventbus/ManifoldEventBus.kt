package io.github.notsyncing.manifold.eventbus

import io.github.notsyncing.manifold.eventbus.event.*
import io.github.notsyncing.manifold.eventbus.exceptions.NodeGroupNotFoundException
import io.github.notsyncing.manifold.eventbus.exceptions.NodeNotFoundException
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramPacket
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.datagram.DatagramSocketOptions
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object ManifoldEventBus {
    var listenPort = 8500
    var beaconInterval = 10000L
    var debug = false

    private var vertx: Vertx? = null

    private val nodes = ConcurrentHashMap<String, ManifoldEventNode>()
    private val groupNodes = ConcurrentHashMap<String, ManifoldEventNodeGroup>()
    private var sendSocket: DatagramSocket? = null
    private var recvSocket: DatagramSocket? = null

    private var beaconTimers = ConcurrentHashMap<String, Timer>()

    fun debug(msg: String) {
        if (debug) {
            System.out.println(msg)
        }
    }

    fun init(vertx: Vertx, listenPort: Int = 8500, beaconInterval: Long = 10000L) {
        this.listenPort = listenPort
        this.beaconInterval = beaconInterval

        this.vertx = vertx

        sendSocket = vertx.createDatagramSocket(DatagramSocketOptions().setBroadcast(true))
        recvSocket = vertx.createDatagramSocket()

        recvSocket?.listen(listenPort, "0.0.0.0") {
            if (it.succeeded()) {
                it.result().handler(this::receivedRemoteEvent)
            } else {
                System.out.println("Failed to receive some packets: " + it.cause().message)
                it.cause().printStackTrace()
            }
        }

        debug("Manifold event bus initialized")
    }

    fun stop(): CompletableFuture<Void> {
        nodes.clear()
        groupNodes.clear()

        beaconTimers.values.forEach { it.cancel() }
        beaconTimers.clear()

        val cs = CompletableFuture<Void>()

        if (sendSocket == null) {
            cs.complete(null)
        } else {
            sendSocket?.close {
                if (it.succeeded()) {
                    sendSocket = null
                    cs.complete(it.result())
                } else {
                    cs.completeExceptionally(it.cause())
                }
            }
        }

        val rs = CompletableFuture<Void>()

        if (recvSocket == null) {
            rs.complete(null)
        } else {
            recvSocket?.close {
                if (it.succeeded()) {
                    recvSocket = null
                    rs.complete(it.result())
                } else {
                    rs.completeExceptionally(it.cause())
                }
            }
        }

        return CompletableFuture.allOf(cs, rs)
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
        nodes.put(node.id, node)

        node.groups?.forEach {
            if (!groupNodes.containsKey(it)) {
                groupNodes.put(it, ManifoldEventNodeGroup(it))
            }

            groupNodes[it]!!.add(node)
        }
    }

    private fun removeNode(id: String) {
        val node = nodes.remove(id)

        if (node?.groups != null) {
            node?.groups?.forEach {
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
        val info = ManifoldEventNode(id, groups as Array<String>, 0, null)
        addNode(info)

        debug("Registered local node $id, groups $groups")

        val beaconTimer = Timer()
        beaconTimer.schedule(object : TimerTask() {
            override fun run() {
                val node = nodes[id]!!

                sendBeacon(node).exceptionally<Boolean> {
                    it.printStackTrace()
                    return@exceptionally null
                }
            }
        }, 0, beaconInterval)

        beaconTimers.put(id, beaconTimer)

        sendBeacon(info)

        return info
    }

    fun unregister(node: ManifoldEventNode): CompletableFuture<Boolean> {
        nodes.remove(node.id)

        node.groups?.forEach {
            if (groupNodes.containsKey(it)) {
                groupNodes[it]!!.remove(node)
            }
        }

        if (beaconTimers.containsKey(node.id)) {
            beaconTimers[node.id]?.cancel()
            beaconTimers.remove(node.id)
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
        val buf = Buffer.buffer(event.serialize())
        val address: String
        val c = CompletableFuture<Boolean>()

        if ((event.sendType == EventSendType.MultiGroupUnicast) || (event.sendType == EventSendType.Broadcast) || (event.sendType == EventSendType.Groupcast)) {
            address = "255.255.255.255"
        } else {
            if (targetNode?.address == null) {
                c.completeExceptionally(RuntimeException("Event $event is unicast-like, but the target node is null or doesn't contain an address!"))
                return c
            }

            address = targetNode!!.address!!
        }

        sendSocket!!.send(buf, listenPort, address) {
            if (it.failed()) {
                c.completeExceptionally(it.cause())
            } else {
                debug("Sent event $event to remote $address")
                c.complete(true)
            }
        }

        return c
    }

    private fun receivedRemoteEvent(packet: DatagramPacket) {
        val data = packet.data().bytes
        val event = ManifoldEvent.parse(data)

        if (event == null) {
            return
        }

        if (nodes[event.source]?.local == true) {
            return
        }

        debug("Received remote event $event from packet (length ${data.size})")

        if (event.type == EventType.Beacon) {
            val beaconEvent = event
            val beaconData = beaconEvent.getData(BeaconData::class.java)

            if (beaconEvent.event == BeaconEvent.Beacon) {
                val node: ManifoldEventNode

                if (nodes.containsKey(beaconData.id)) {
                    node = nodes[beaconData.id]!!

                    if (node.address == null) {
                        return
                    }

                    node.groups = beaconData.groups
                    node.load = beaconData.load
                    node.address = packet.sender().host()
                } else {
                    node = ManifoldEventNode(beaconData.id, beaconData.groups, beaconData.load, packet.sender().host())
                    addNode(node)
                }
            } else if (beaconEvent.event == BeaconEvent.Exit) {
                removeNode(beaconData.id)
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