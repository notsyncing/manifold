package io.github.notsyncing.manifold.story.drama.engine

import jdk.nashorn.api.scripting.ScriptObjectMirror
import java.io.Reader
import java.nio.file.Path
import javax.script.Invocable
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class NashornDramaEngine : DramaEngine() {
    companion object {
        const val DRAMA_EXT = ".drama.js"

        @JvmStatic
        fun toCallable(obj: ScriptObjectMirror): CallableObject {
            return NashornCallableObject(obj)
        }
    }

    private val engine: ScriptEngine

    init {
        engine = ScriptEngineManager().getEngineByName("nashorn")

        engine.eval("load('classpath:manifold_story/drama-lib-nashorn.js')")
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
        val path = file.toAbsolutePath().normalize().toString()

        engine.eval("load('${path.replace("\\", "\\\\")}')")
    }

    override fun loadScriptFromClasspath(file: String) {
        engine.eval("load('classpath:$file')")
    }
}