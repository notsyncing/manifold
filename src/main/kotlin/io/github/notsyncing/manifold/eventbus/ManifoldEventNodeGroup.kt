package io.github.notsyncing.manifold.eventbus

import io.github.notsyncing.manifold.eventbus.event.EventSendType
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import java.util.*

class ManifoldEventNodeGroup(val name: String) {
    val nodes = ArrayList<ManifoldEventNode>()
    var currIndex = 0
    var count = 0
        get() = nodes.size

    fun add(node: ManifoldEventNode) = nodes.add(node)

    fun remove(node: ManifoldEventNode) = nodes.remove(node)

    fun receive(event: ManifoldEvent) {
        if (nodes.size <= 0) {
            return
        }

        if ((event.sendType == EventSendType.GroupUnicast) || (event.sendType == EventSendType.MultiGroupUnicast)) {
            var i = currIndex
            currIndex++

            if (currIndex >= nodes.size) {
                currIndex = 0
            }

            if (i >= nodes.size) {
                i = nodes.size - 1
            }

            nodes[i].receive(event)
            return
        } else if (event.sendType == EventSendType.Groupcast) {
            nodes.forEach {
                it.receive(event)
            }

            return
        } else {
            throw RuntimeException("Group $name received non-group event $event")
        }
    }
}