package io.github.notsyncing.manifold.story.drama.engine.nashorn

import io.github.notsyncing.manifold.story.drama.engine.CallableObject
import jdk.nashorn.api.scripting.ScriptObjectMirror

class NashornCallableObject(private val obj: ScriptObjectMirror) : CallableObject {
    override fun call(thiz: Any?, vararg parameters: Any?): Any? {
        return obj.call(thiz, *parameters)
    }
}