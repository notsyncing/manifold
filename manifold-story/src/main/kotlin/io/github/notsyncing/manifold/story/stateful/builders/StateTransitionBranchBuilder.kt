package io.github.notsyncing.manifold.story.stateful.builders

import io.github.notsyncing.manifold.story.stateful.StateTransitionItem

class StateTransitionBranchBuilder(private val itemRef: StateTransitionItem) {
    companion object {
        val other: (Any?) -> Boolean = { _ -> false }
        val exception: (Any?) -> Boolean = { _ -> false }
    }

    val other = Companion.other
    val exception = Companion.exception

    private val list = mutableListOf<StateTransitionBranchItemBuilder>()

    fun on(cond: (Any?) -> Boolean): StateTransitionBranchItemBuilder {
        val b = StateTransitionBranchItemBuilder(itemRef, cond)
        list.add(b)
        return b
    }

    fun build() = list.map { it.build() }
}