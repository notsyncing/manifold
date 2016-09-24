package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.eventbus.EventBusNetWorker
import io.github.notsyncing.manifold.eventbus.ManifoldEventNode
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import io.github.notsyncing.manifold.utils.StreamUtils
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

@RunWith(VertxUnitRunner::class)
class EventBusNetWorkerTest {
    private lateinit var vertx: Vertx
    private lateinit var fakeEventNode: ManifoldEventNode
    private lateinit var worker: EventBusNetWorker

    @Before
    fun setUp() {
        vertx = Vertx.vertx()

        fakeEventNode = ManifoldEventNode("test", host = "127.0.0.1", port = 8501)
        worker = EventBusNetWorker(8500, 8500) { ev, host -> }
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
                        c.complete(event)
                    }
                }
            } else {
                c.completeExceptionally(it.cause())
            }
        }

        return c
    }

    @Test
    fun testSendShortStringThroughUdp(context: TestContext) {
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
    fun testSendShortStreamThroughUdp(context: TestContext) {
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
}