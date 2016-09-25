package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.eventbus.EventBusNetWorker
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.utils.ReadInputStream
import io.github.notsyncing.manifold.utils.StreamUtils
import io.github.notsyncing.manifold.utils.WriteOutputStream
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

@RunWith(VertxUnitRunner::class)
class EventBusNetWorkerTest {
    private lateinit var vertx: Vertx
    private lateinit var fakeEventNode: ManifoldEventNode
    private lateinit var worker: EventBusNetWorker
    private var recvEvent: CompletableFuture<ManifoldEvent> = CompletableFuture()

    @Before
    fun setUp() {
        vertx = Vertx.vertx()
        recvEvent = CompletableFuture()

        fakeEventNode = ManifoldEventNode("test", host = "127.0.0.1", port = 8501)
        worker = EventBusNetWorker(8500, 8500) { ev, host -> recvEvent.complete(ev) }
    }

    @After
    fun tearDown() {
        worker.close().thenCompose {
            EventBusNetWorker.close()
        }.thenCompose {
            val c = CompletableFuture<Void>()
            vertx.close { c.complete(null) }

            return@thenCompose c
        }.get()
    }

    private fun listenUdpForEvent(parseStream: Boolean = false): CompletableFuture<ManifoldEvent?> {
        val c = CompletableFuture<ManifoldEvent?>()
        val rs = vertx.createDatagramSocket()

        rs.listen(8501, "0.0.0.0") {
            if (it.succeeded()) {
                val p = it.result()

                p.handler {
                    val event: ManifoldEvent?

                    if (parseStream) {
                        ManifoldEvent.parse(DataInputStream(ByteArrayInputStream(it.data().bytes)))
                                .thenAccept {
                                    c.complete(it)
                                }.exceptionally {
                                    c.completeExceptionally(it.cause)
                                    return@exceptionally null
                                }
                    } else {
                        event = ManifoldEvent.parse(it.data().bytes)

                        rs.close {
                            c.complete(event)
                        }
                    }
                }
            } else {
                c.completeExceptionally(it.cause())
            }
        }

        return c
    }

    private fun listenTcpForEvent(): Pair<CompletableFuture<Void>, CompletableFuture<ManifoldEvent?>> {
        val ce = CompletableFuture<ManifoldEvent?>()
        val cc = CompletableFuture<Void>()
        val server = vertx.createNetServer()

        server.connectHandler { socket ->
            ForkJoinPool.commonPool().submit {
                val rs = ReadInputStream(socket)
                val s = DataInputStream(rs)

                ManifoldEvent.parse(s).thenAccept { e ->
                    socket.close()

                    server.close {
                        ce.complete(e)
                    }
                }
            }
        }

        server.listen(8501, "0.0.0.0") {
            if (it.failed()) {
                cc.completeExceptionally(it.cause())
            } else {
                cc.complete(null)
            }
        }

        return Pair(cc, ce)
    }

    @Test
    fun testSendStringThroughUdp(context: TestContext) {
        EventBusNetWorker.UDP_DATA_LENGTH_LIMIT = 256
        val event = ManifoldEvent("event", "Test data")
        val async = context.async()

        listenUdpForEvent().thenAccept {
            context.assertNotNull(it)
            context.assertEquals("Test data", it!!.data)
            async.complete()
        }.exceptionally {
            context.fail(it)
            return@exceptionally null
        }

        worker.send(event, fakeEventNode).exceptionally {
            context.fail(it)
            return@exceptionally false
        }
    }

    @Test
    fun testSendStreamThroughUdp(context: TestContext) {
        EventBusNetWorker.UDP_DATA_LENGTH_LIMIT = 256
        val s = "Test data 2"
        val event = ManifoldEvent("event", StreamUtils.stringToStream(s), s.toByteArray(StandardCharsets.UTF_8).size.toLong())
        val async = context.async()

        listenUdpForEvent(true).thenAccept {
            context.assertNotNull(it)
            context.assertNotNull(it!!.dataStream)
            context.assertEquals(s, StreamUtils.streamToString(it.dataStream!!))
            async.complete()
        }.exceptionally {
            context.fail(it)
            return@exceptionally null
        }

        worker.send(event, fakeEventNode).exceptionally {
            context.fail(it)
            return@exceptionally false
        }
    }

    @Test
    fun testSendStringThroughTcp(context: TestContext) {
        EventBusNetWorker.UDP_DATA_LENGTH_LIMIT = 1
        val event = ManifoldEvent("event", "Test data 3")
        val async = context.async()

        val (cc, ce) = listenTcpForEvent()

        ce.thenAccept {
            context.assertNotNull(it)
            context.assertEquals("Test data 3", it!!.data)
            async.complete()
        }.exceptionally {
            context.fail(it)
            return@exceptionally null
        }

        cc.thenAccept {
            worker.send(event, fakeEventNode).exceptionally {
                context.fail(it)
                return@exceptionally false
            }
        }
    }

    @Test
    fun testSendStreamThroughTcp(context: TestContext) {
        EventBusNetWorker.UDP_DATA_LENGTH_LIMIT = 1
        val s = "Test data 4"
        val event = ManifoldEvent("event", StreamUtils.stringToStream(s), s.toByteArray(StandardCharsets.UTF_8).size.toLong())
        val async = context.async()

        val (cc, ce) = listenTcpForEvent()

        ce.thenAccept {
            context.assertNotNull(it)
            context.assertNotNull(it!!.dataStream)
            context.assertEquals(s, StreamUtils.streamToString(it.dataStream!!, it.dataLength.toInt()))
            async.complete()
        }.exceptionally {
            context.fail(it)
            return@exceptionally null
        }

        cc.thenAccept {
            worker.send(event, fakeEventNode).exceptionally {
                context.fail(it)
                return@exceptionally false
            }
        }
    }

    private fun sendEventUdp(event: ManifoldEvent): CompletableFuture<Void> {
        val c = CompletableFuture<Void>()
        val s = vertx.createDatagramSocket()

        s.send(Buffer.buffer(event.serialize()), 8500, "127.0.0.1") {
            if (it.succeeded()) {
                c.complete(null)
            } else {
                c.completeExceptionally(it.cause())
            }

            s.close()
        }

        return c
    }

    private fun sendEventTcp(event: ManifoldEvent, useStream: Boolean = false): CompletableFuture<Void> {
        val c = CompletableFuture<Void>()
        val n = vertx.createNetClient()

        n.connect(8500, "127.0.0.1") {
            if (it.failed()) {
                c.completeExceptionally(it.cause())
                return@connect
            }

            if (useStream) {
                val ws = WriteOutputStream(it.result())
                val s = DataOutputStream(ws)

                event.serialize(s).thenAccept {
                    ws.close()

                    c.complete(null)

                    n.close()
                }
            } else {
                val buf = event.serialize()
                it.result().write(Buffer.buffer(buf))

                c.complete(null)

                n.close()
            }
        }

        return c
    }

    @Test
    fun testRecvStringThroughUdp(context: TestContext) {
        val event = ManifoldEvent("event", "Test data 5")
        val async = context.async()

        sendEventUdp(event).thenCompose {
            recvEvent
        }.thenAccept {
            context.assertEquals("Test data 5", it.data)
            async.complete()
        }.get(5, TimeUnit.SECONDS)
    }

    @Test
    fun testRecvStringThroughTcp(context: TestContext) {
        val event = ManifoldEvent("event", "Test data 6")
        val async = context.async()

        sendEventTcp(event).thenCompose {
            recvEvent
        }.thenAccept {
            context.assertEquals("Test data 6", it.data)
            async.complete()
        }.get(5, TimeUnit.SECONDS)
    }

    @Test
    fun testRecvStreamThroughTcp(context: TestContext) {
        val s = "Test data 7"
        val event = ManifoldEvent("event", StreamUtils.stringToStream(s), s.toByteArray(StandardCharsets.UTF_8).size.toLong())
        val async = context.async()

        sendEventTcp(event, true).thenCompose {
            recvEvent
        }.thenAccept {
            context.assertNotNull(it.dataStream)
            context.assertEquals("Test data 7", StreamUtils.streamToString(it.dataStream!!, it.dataLength.toInt()))
            async.complete()
        }.get(5, TimeUnit.SECONDS)
    }
}