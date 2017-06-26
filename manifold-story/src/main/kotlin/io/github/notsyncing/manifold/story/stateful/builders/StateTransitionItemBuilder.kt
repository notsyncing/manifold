package io.github.notsyncing.manifold.story.stateful.builders

import io.github.notsyncing.manifold.story.stateful.StateTransitionBranchItem
import io.github.notsyncing.manifold.story.stateful.StateTransitionItem

class StateTransitionItemBuilder(private val itemRef: StateTransitionItem) {
    infix fun at(state: Enum<*>): StateTransitionItemBuilder {
        itemRef.at = state
        return this
    }

    infix fun transitionTo(state: Enum<*>): StateTransitionItemBuilder {
        itemRef.branches = listOf(StateTransitionBranchItem({ _ -> true }, state))
        return this
    }

    infix fun transitions(builder: StateTransitionBranchBuilder.() -> Unit): StateTransitionItemBuilder {
        val b = StateTransitionBranchBuilder(itemRef)
        b.builder()
        itemRef.branches = b.build()

        return this
    }

    infix fun handler(h: () -> Any?): StateTransitionItemBuilder {
        itemRef.handler = h
        return this
    }

    infix fun self(looped: Any): StateTransitionItemBuilder {
        return transitionTo(itemRef.at)
    }
}