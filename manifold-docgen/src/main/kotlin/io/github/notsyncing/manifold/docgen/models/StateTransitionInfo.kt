package io.github.notsyncing.manifold.docgen.models

import io.github.notsyncing.manifold.story.stateful.StateTransitionItem

class StateTransitionInfo(var from: String,
                          var on: String,
                          var branches: List<StateTransitionBranchInfo>) {
    constructor(item: StateTransitionItem) : this("", "", emptyList()) {
        from = item.at.name
        on = item.on.event
        branches = item.branches.map { StateTransitionBranchInfo(it) }
    }
}