package io.github.notsyncing.manifold.docgen.models

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.story.stateful.StateMachine
import io.github.notsyncing.manifold.story.stateful.StatefulScene

open class StatefulSceneInfo(name: String,
                             fullName: String,
                             description: String,
                             parameters: List<ParameterInfo>,
                             var states: List<StateInfo>,
                             var transitions: List<StateTransitionInfo>) : SceneInfo(name, fullName, description, parameters) {
    constructor(clazz: Class<StatefulScene<*>>) : this("", "", "", emptyList(), emptyList(), emptyList()) {
        fillStateful(clazz)
    }

    protected fun fillStateful(clazz: Class<StatefulScene<*>>) {
        fill(clazz as Class<ManifoldScene<*>>)

        val transitionMap = StateMachine.stateTransitionMapCache[clazz as Class<StateMachine<*>>]

        if (transitionMap == null) {
            throw RuntimeException("No transition map cache found for class $clazz")
        }

        if (transitionMap.isEmpty()) {
            throw RuntimeException("Class $clazz has empty transition map!")
        }

        states = transitionMap.map { it.at.javaClass.enumConstants.map { StateInfo(it) } }
                .flatten()
                .distinctBy { it.realName }

        transitions = transitionMap.map { StateTransitionInfo(it) }
    }
}