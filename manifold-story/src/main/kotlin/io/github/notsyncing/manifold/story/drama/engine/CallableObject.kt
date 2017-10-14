package io.github.notsyncing.manifold.story.drama.engine

interface CallableObject {
    fun call(thiz: Any?, vararg parameters: Any?): Any?
}