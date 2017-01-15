package io.github.notsyncing.manifold.eventbus.workers

import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.EventSendType
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.utils.FutureUtils
import io.github.notsyncing.manifold.utils.ReadInputStream
import io.github.notsyncing.manifold.utils.WriteOutputStream
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.datagram.DatagramPacket
import io.vertx.core.datagram.DatagramSocket
import io.vertx.core.datagram.DatagramSocketOptions
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class EventBusNetWorker(val tcpListenPort: Int, val udpListenPort: Int,
                        val eventRecvHandler: ((TransportDescriptor, ManifoldEvent, String) -> Unit)? = null) {
    companion object {
        var UDP_DATA_LENGTH_LIMIT = 256

        private val transDesc = NetTransport(null, 0)

        private var vertx: Vertx? = Vertx.vertx()

        fun close(): CompletableFuture<Void> {
            if (vertx == null) {
                return CompletableFuture.completedFuture(null)
            }

            val c = CompletableFuture<Void>()

            vertx!!.close {
                if (it.failed()) {
                    c.completeExceptionally(it.cause())
                } else {
                    vertx = null
                    c.complete(it.result())
                }
            }

            return c
        }
    }

    private val udpSendSocket: DatagramSocket
    private val udpRecvSocket: DatagramSocket
    private val tcpServer: NetServer
    private val tcpConnectionCache = ConcurrentHashMap<String, NetSocket>()
    private val threadPool = Executors.newFixedThreadPool(20)

    init {
        if (vertx == null) {
            vertx = Vertx.vertx()
        }

        udpSendSocket = vertx!!.createDatagramSocket(DatagramSocketOptions().setBroadcast(true))
        udpRecvSocket = vertx!!.createDatagramSocket()

        udpRecvSocket?.listen(udpListenPort, "0.0.0.0") {
            if (it.succeeded()) {
                it.result().handler(this::onUdpReceived)
            } else {
                System.out.println("Failed listen on UDP port $udpListenPort: ${it.cause().message}")
                it.cause().printStackTrace()
            }
        }

        val tcpOpts = NetServerOptions()
                .setHost("0.0.0.0")
                .setPort(tcpListenPort)

        tcpServer = vertx!!.createNetServer(tcpOpts)

        tcpServer.connectHandler {
            val address = "${it.remoteAddress().host()}:${it.remoteAddress().port()}"
            tcpConnectionCache[address] = it

            it.closeHandler { tcpConnectionCache.remove(address) }

            onTcpInboundConnected(it)
        }

        tcpServer.listen {
            if (!it.succeeded()) {
                System.out.println("Failed listen on TCP port $tcpListenPort: " + it.cause().message)
                it.cause().printStackTrace()
            }
        }
    }

    private fun onUdpReceived(packet: DatagramPacket) {
        val data = packet.data().bytes
        val event = ManifoldEvent.parse(data)

        if (event == null) {
            return
        }

        eventRecvHandler?.invoke(transDesc, event, packet.sender().host())
    }

    private fun onTcpInboundConnected(socket: NetSocket) {
        threadPool.submit {
            val stream = ReadInputStream(socket)
            val ds = DataInputStream(stream)

            while ((!stream.ended) && (!threadPool.isShutdown)) {
                ManifoldEvent.parse(ds).thenAccept {
                    stream.close()

                    if (it != null) {
                        eventRecvHandler?.invoke(transDesc, it, socket.remoteAddress().host())
                    }
                }
            }
        }
    }

    fun close(): CompletableFuture<Void> {
        threadPool.shutdown()

        tcpConnectionCache.values.forEach { it.close() }
        tcpConnectionCache.clear()

        val cs = CompletableFuture<Void>()

        udpSendSocket.close {
            if (it.succeeded()) {
                cs.complete(it.result())
            } else {
                cs.completeExceptionally(it.cause())
            }
        }

        val rs = CompletableFuture<Void>()

        udpRecvSocket.close {
            if (it.succeeded()) {
                rs.complete(it.result())
            } else {
                rs.completeExceptionally(it.cause())
            }
        }

        val ts = CompletableFuture<Void>()

        tcpServer.close {
            if (it.succeeded()) {
                ts.complete(it.result())
            } else {
                ts.completeExceptionally(it.cause())
            }
        }

        return CompletableFuture.allOf(cs, rs, ts)
    }

    private fun sendUdp(data: Buffer, port: Int, host: String): CompletableFuture<Boolean> {
        val c = CompletableFuture<Boolean>()

        udpSendSocket.send(data, port, host) {
            if (it.failed()) {
                c.completeExceptionally(it.cause())
            } else {
                c.complete(true)
            }
        }

        return c
    }

    private fun getTcpConnection(port: Int, host: String): CompletableFuture<NetSocket> {
        val c = CompletableFuture<NetSocket>()
        val address = "$host:$port"

        if (tcpConnectionCache.containsKey(address)) {
            return CompletableFuture.completedFuture(tcpConnectionCache[address]!!)
        } else {
            val client = vertx!!.createNetClient()

            client.connect(port, host) {
                if (it.succeeded()) {
                    val s = it.result()

                    s.closeHandler {
                        tcpConnectionCache.remove(host)
                        client.close()
                    }

                    tcpConnectionCache[host] = s
                    c.complete(s)
                } else {
                    val ex = IOException("Failed to connect to node $host port $port " + it.cause().message, it.cause())
                    c.completeExceptionally(ex)
                }
            }
        }

        return c
    }

    private fun sendTcp(event: ManifoldEvent, port: Int, host: String): CompletableFuture<Boolean> {
        var ws: WriteOutputStream? = null

        return getTcpConnection(port, host).thenCompose {
            ws = WriteOutputStream(it)
            val s = DataOutputStream(ws)

            return@thenCompose event.serialize(s, threadPool)
        }.thenApply {
            ws?.close()
            true
        }
    }

    fun send(event: ManifoldEvent, targetNode: ManifoldEventNode? = null): CompletableFuture<Boolean> {
        val address: String
        var port: Int = 0
        var isMulticast = false

        if ((event.sendType == EventSendType.MultiGroupUnicast) || (event.sendType == EventSendType.Broadcast) || (event.sendType == EventSendType.Groupcast)) {
            address = "255.255.255.255"
            isMulticast = true
        } else {
            if (targetNode == null) {
                return FutureUtils.failed(RuntimeException("Target node of event $event is null!"))
            }

            if (targetNode.transport !is NetTransport) {
                return FutureUtils.failed(RuntimeException("Target node $targetNode of event $event cannot accept ${NetTransport::class.java.simpleName}!"))
            }

            val trans = targetNode.transport as NetTransport

            if (trans.host == null) {
                return FutureUtils.failed(RuntimeException("Event $event is unicast-like, but its target node is null or doesn't contain an address!"))
            }

            address = trans.host
            port = trans.port
        }

        val len = event.dataLength

        if (!isMulticast) {
            if (port == 0) {
                port = tcpListenPort
            }

            if (len > UDP_DATA_LENGTH_LIMIT) {
                return sendTcp(event, port, address)
            }
        }

        if (port == 0) {
            port = udpListenPort
        }

        val buf = Buffer.buffer(event.serialize())
        return sendUdp(buf, port, address)
    }
}