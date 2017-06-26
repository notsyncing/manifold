package io.github.notsyncing.manifold.docgen.models

import io.github.notsyncing.manifold.story.stateful.StateTransitionBranchItem
import io.github.notsyncing.manifold.story.stateful.builders.StateTransitionBranchBuilder

class StateTransitionBranchInfo(var cond: String,
                                var to: String) {
    constructor(item: StateTransitionBranchItem) : this("", "") {
        cond = when (item.condition) {
            StateTransitionBranchBuilder.exception -> "exception"
            StateTransitionBranchBuilder.other -> "other"
            else -> item.condition.toString()
        }

        to = item.to.name
    }
}