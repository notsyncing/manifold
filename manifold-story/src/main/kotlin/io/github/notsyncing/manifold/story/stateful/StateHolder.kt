package io.github.notsyncing.manifold.story.stateful

open class StateHolder {
    var currState: Enum<*> = DefaultState.Entry
}