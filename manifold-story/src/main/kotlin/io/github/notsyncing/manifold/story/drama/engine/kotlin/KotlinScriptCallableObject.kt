package io.github.notsyncing.manifold.story.drama.engine.kotlin

import io.github.notsyncing.manifold.story.drama.engine.CallableObject
import java.lang.reflect.Method

class KotlinScriptCallableObject(private val obj: Function<*>) : CallableObject {
    private val entry: Method

    init {
        entry = obj.javaClass.methods.first { it.name == "invoke" }.apply { this.isAccessible = true }
    }

    override fun call(thiz: Any?, vararg parameters: Any?): Any? {
        return entry.invoke(obj, *parameters)
    }
}