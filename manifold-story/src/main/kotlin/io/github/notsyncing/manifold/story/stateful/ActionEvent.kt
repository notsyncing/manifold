package io.github.notsyncing.manifold.story.stateful

import io.github.notsyncing.manifold.action.ActionMetadata

class ActionEvent(val actionName: String) : StateEvent() {
    companion object {
        inline fun <reified T> action() = ActionEvent(T::class.java.getAnnotation(ActionMetadata::class.java)?.value ?: T::class.java.name)
    }

    override val event: String
        get() = actionName
}