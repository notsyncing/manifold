package io.github.notsyncing.manifold.story.stateful.builders

import io.github.notsyncing.manifold.story.stateful.StateTransitionBranchItem
import io.github.notsyncing.manifold.story.stateful.StateTransitionItem

class StateTransitionBranchItemBuilder(private val itemRef: StateTransitionItem,
                                       private val cond: (Any?) -> Boolean) {
    private lateinit var state: Enum<*>
    private var afterHandler: (Any?) -> Any? = { it }

    infix fun to(state: Enum<*>): StateTransitionBranchItemBuilder {
        this.state = state
        return this
    }

    infix fun self(looped: Any): StateTransitionBranchItemBuilder {
        return to(itemRef.at)
    }

    infix fun then(handler: (Any?) -> Any?): StateTransitionBranchItemBuilder {
        this.afterHandler = handler
        return this
    }

    fun build() = StateTransitionBranchItem(cond, state, afterHandler)
}