package io.github.notsyncing.manifold.story.stateful.exceptions

import io.github.notsyncing.manifold.story.stateful.StateEvent

class NoBranchTransitionFoundException(r: Any?, currState: Enum<*>, event: StateEvent) :
        Exception("No branch transition found for result $r from current state $currState at event $event") {
}