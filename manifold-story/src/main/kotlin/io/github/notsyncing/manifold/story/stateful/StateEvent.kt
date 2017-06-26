package io.github.notsyncing.manifold.story.stateful

abstract class StateEvent {
    abstract val event: String

    override fun toString(): String {
        return event
    }

    override fun hashCode(): Int {
        return event.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateEvent) {
            return event == other.event
        } else {
            return super.equals(other)
        }
    }
}