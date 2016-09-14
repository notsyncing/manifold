package io.github.notsyncing.manifold.eventbus.event

enum class EventSendType {
    Unicast,
    Groupcast,
    Broadcast,
    GroupUnicast,
    MultiGroupUnicast
}
