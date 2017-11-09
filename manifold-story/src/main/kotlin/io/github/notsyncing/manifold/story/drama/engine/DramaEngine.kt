package io.github.notsyncing.manifold.story.drama.engine

import java.io.Reader
import java.nio.file.Path

abstract class DramaEngine {
    abstract fun eval(script: String): Any?

    abstract fun eval(script: Reader): Any?

    abstract fun setContextAttribute(name: String, value: Any?)

    abstract fun removeContextAttribute(name: String): Any?

    abstract fun get(name: String): Any?

    abstract fun invoke(obj: Any?, method: String, vararg parameters: Any?): Any?

    abstract fun loadScript(file: Path): Any?

    abstract fun loadScriptFromClasspath(file: String): Any?
}