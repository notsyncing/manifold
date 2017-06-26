package io.github.notsyncing.manifold.story.stateful

class StateTransitionBranchItem(val condition: (Any?) -> Boolean,
                                val to: Enum<*>,
                                val afterHandler: ((Any?) -> Any?) = { it }) {
}