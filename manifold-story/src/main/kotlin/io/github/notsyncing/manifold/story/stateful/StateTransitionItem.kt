package io.github.notsyncing.manifold.story.stateful

data class StateTransitionItem(val on: StateEvent,
                               var at: Enum<*> = DefaultState.Unknown,
                               var handler: (() -> Any?)? = null,
                               var branches: List<StateTransitionBranchItem> = listOf())