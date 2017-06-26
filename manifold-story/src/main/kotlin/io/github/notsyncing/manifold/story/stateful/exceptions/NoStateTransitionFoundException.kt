package io.github.notsyncing.manifold.story.stateful.exceptions

import io.github.notsyncing.manifold.story.stateful.StateEvent
import io.github.notsyncing.manifold.story.stateful.StateMachine

class NoStateTransitionFoundException(stateMachine: StateMachine<*>, event: StateEvent, currentState: Enum<*>) :
        Exception("Transition from current state $currentState at event $event not found in state machine $stateMachine") {
}