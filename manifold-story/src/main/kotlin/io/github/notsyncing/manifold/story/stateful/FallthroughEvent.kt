package io.github.notsyncing.manifold.story.stateful

class FallthroughEvent : StateEvent() {
    override val event: String
        get() = "manifold.story.statemachine.events.fallthrough"
}