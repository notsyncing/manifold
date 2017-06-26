package io.github.notsyncing.manifold.docgen.models

import io.github.notsyncing.manifold.story.stateful.StateMetadata

class StateInfo(var name: String,
                var realName: String,
                var description: String) {
    constructor(enum: Enum<*>) : this("", "", "") {
        val metadata = enum.javaClass.getField(enum.name).getAnnotation(StateMetadata::class.java)
        name = metadata?.value ?: ""
        realName = enum.name
        description = metadata?.description ?: ""
    }
}