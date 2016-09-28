package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.eventbus.event.EventSendType
import io.github.notsyncing.manifold.eventbus.event.EventType
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import org.junit.Assert
import org.junit.Test

class ManifoldEventTest {
    @Test
    fun testParseAndSerializeWithStringData() {
        val e = ManifoldEvent("Test event", "test data")
        e.sessionId = "test_session"
        e.source = "source_node"
        e.target = "target_node"
        e.sendType = EventSendType.GroupUnicast
        e.type = EventType.Beacon

        val e2 = ManifoldEvent.parse(e.serialize())

        Assert.assertNotNull(e2)
        Assert.assertEquals("Test event", e2!!.event)
        Assert.assertEquals("test data", e2.data)
        Assert.assertEquals("test_session", e2.sessionId)
        Assert.assertEquals("source_node", e2.source)
        Assert.assertEquals("target_node", e2.target)
        Assert.assertEquals(EventSendType.GroupUnicast, e2.sendType)
        Assert.assertEquals(EventType.Beacon, e2.type)
    }
}