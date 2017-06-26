package io.github.notsyncing.manifold.story.stateful

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.suspendable.SuspendableScene
import io.github.notsyncing.manifold.suspendable.WaitStrategy
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import kotlin.reflect.jvm.javaField

abstract class StatefulScene<S: StateHolder> : SuspendableScene<Any?>(), StateMachine<S> {
    override val defaultTransitionHandler: () -> Any?
        get() = this::actionHandler

    override fun serialize(): JSONObject {
        return JSONObject()
                .fluentPut("state", JSON.toJSON(stateHolder))
    }

    override fun deserialize(o: JSONObject) {
        stateHolder = o.getObject("state", this::stateHolder.javaField!!.type) as S
    }

    override fun stage() = future {
        super.stage().await()

        if ((lastActionClass == null) && (stateHolder.currState == DefaultState.Entry)) {
            registerNextPossibleActions()
        }

        var r = lastResult

        if (lastActionClass != null) {
            r = step(ActionEvent(lastActionClass!!.name)).await()
        }

        r
    }

    protected fun actionHandler(): Any? {
        if (lastException != null) {
            throw lastException!!
        }

        return lastResult
    }

    private fun registerNextPossibleActions() {
        val nextPossibleActions = getAllPossibleEventsAtCurrentState()
                .filter { it is ActionEvent }
                .map { Class.forName(it.event) as Class<ManifoldAction<*>> }

        awaitFor(WaitStrategy.Or, *nextPossibleActions.toTypedArray())
    }

    override fun afterStep(event: StateEvent) {
        super.afterStep(event)

        registerNextPossibleActions()
    }
}