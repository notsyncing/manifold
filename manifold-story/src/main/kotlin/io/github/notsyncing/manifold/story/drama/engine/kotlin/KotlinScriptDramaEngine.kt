package io.github.notsyncing.manifold.story.drama.engine.kotlin

import io.github.notsyncing.manifold.story.drama.engine.CallableObject
import io.github.notsyncing.manifold.story.drama.engine.DramaEngine
import java.io.InputStreamReader
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import javax.script.Invocable
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class KotlinScriptDramaEngine : DramaEngine() {
    companion object {
        const val DRAMA_EXT = ".drama.kts"

        fun toCallable(obj: Function<*>): CallableObject {
            return KotlinScriptCallableObject(obj)
        }
    }

    private val engine: ScriptEngine

    init {
        engine = ScriptEngineManager().getEngineByExtension("kts")
    }

    override fun eval(script: String): Any? {
        return engine.eval(script)
    }

    override fun eval(script: Reader): Any? {
        return engine.eval(script)
    }

    override fun setContextAttribute(name: String, value: Any?) {
        engine.context.setAttribute(name, value, ScriptContext.ENGINE_SCOPE)
    }

    override fun removeContextAttribute(name: String): Any? {
        return engine.context.removeAttribute(name, ScriptContext.ENGINE_SCOPE)
    }

    override fun get(name: String): Any? {
        return engine.get(name)
    }

    override fun invoke(obj: Any?, method: String, vararg parameters: Any?): Any? {
        return (engine as Invocable).invokeMethod(obj, method, *parameters)
    }

    override fun loadScript(file: Path) {
        InputStreamReader(Files.newInputStream(file)).use {
            eval(it)
        }
    }

    override fun loadScriptFromClasspath(file: String) {
        InputStreamReader(javaClass.getResourceAsStream("/" + file)).use {
            eval(it)
        }
    }
}