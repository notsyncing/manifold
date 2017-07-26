package io.github.notsyncing.manifold.story.stateful

import io.github.notsyncing.manifold.story.stateful.builders.StateTransitionBranchBuilder
import io.github.notsyncing.manifold.story.stateful.builders.StateTransitionItemBuilder
import io.github.notsyncing.manifold.story.stateful.exceptions.NoBranchTransitionFoundException
import io.github.notsyncing.manifold.story.stateful.exceptions.NoStateTransitionFoundException
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

interface StateMachine<S: StateHolder> {
    companion object {
        val stateTransitionMapCache = ConcurrentHashMap<Class<StateMachine<*>>, MutableList<StateTransitionItem>>()
    }

    var stateHolder: S

    val defaultTransitionHandler: () -> Any?
        get() = this::defHandler

    val looped: Unit get() = Unit

    val stateTransitionMap: MutableList<StateTransitionItem> get() {
        var map = stateTransitionMapCache[javaClass as Class<StateMachine<*>>]

        if (map == null) {
            map = mutableListOf()
            stateTransitionMapCache[javaClass as Class<StateMachine<*>>] = map
        }

        return map
    }

    private fun defHandler() {}

    fun <T: StateMachine<S>> build(machine: T, content: T.() -> Unit) {
        content.invoke(machine)
    }

    fun on(event: StateEvent): StateTransitionItemBuilder {
        val item = StateTransitionItem(event)
        item.handler = defaultTransitionHandler
        stateTransitionMap.add(item)

        return StateTransitionItemBuilder(item)
    }

    fun beforeStep(event: StateEvent) {}

    fun step(event: StateEvent) = future {
        beforeStep(event)

        val transItem = stateTransitionMap.firstOrNull { (it.on == event) && (it.at == stateHolder.currState) }

        if (transItem == null) {
            throw NoStateTransitionFoundException(this@StateMachine, event, stateHolder.currState)
        }

        var r: Any?
        var exception: Exception? = null

        try {
            val result = transItem.handler?.invoke()

            if (result is CompletableFuture<*>) {
                r = result.await()
            } else {
                r = result
            }
        } catch (e: Exception) {
            exception = e
            r = null
        }

        var currBranch: StateTransitionBranchItem? = null

        for (branch in transItem.branches) {
            if (branch.condition == StateTransitionBranchBuilder.exception) {
                if (exception != null) {
                    stateHolder.currState = branch.to
                    currBranch = branch
                    break
                }
            }

            if ((exception == null) && (branch.condition(r))) {
                stateHolder.currState = branch.to
                currBranch = branch
                break
            }
        }

        if (currBranch == null) {
            val otherwiseBranch = transItem.branches.firstOrNull { it.condition == StateTransitionBranchBuilder.other }

            if (otherwiseBranch == null) {
                throw NoBranchTransitionFoundException(r, stateHolder.currState, event)
            }

            stateHolder.currState = otherwiseBranch.to
            currBranch = otherwiseBranch
        }

        val d = currBranch.afterHandler(r ?: exception)

        if (r != null) {
            r = d
        }

        fallthrough()

        afterStep(event)

        r
    }

    private fun fallthrough() {
        val ftItem = stateTransitionMap.firstOrNull { (it.on is FallthroughEvent) && (it.at == stateHolder.currState) }

        if (ftItem != null) {
            stateHolder.currState = ftItem.branches[0].to

            fallthrough()
        }
    }

    fun afterStep(event: StateEvent) {}

    fun getAllPossibleEventsAtState(state: Enum<*>): List<StateEvent> {
        return stateTransitionMap.filter { it.at == state }
                .map { it.on }
    }

    fun getAllPossibleEventsAtCurrentState() = getAllPossibleEventsAtState(stateHolder.currState)
}