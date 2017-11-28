package io.github.notsyncing.manifold.story.drama.engine.kotlin

import com.intellij.openapi.util.Disposer
import io.github.notsyncing.manifold.story.drama.engine.CallableObject
import io.github.notsyncing.manifold.story.drama.engine.DramaEngine
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import org.jetbrains.kotlin.script.jsr223.KotlinStandardJsr223ScriptTemplate
import java.io.InputStreamReader
import java.io.Reader
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.script.*

class KotlinScriptDramaEngine : DramaEngine() {
    companion object {
        const val DRAMA_EXT = ".drama.kts"

        fun toCallable(obj: Function<*>): CallableObject {
            return KotlinScriptCallableObject(obj)
        }
    }

    private class EngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {
        override fun getScriptEngine(): ScriptEngine {
            val classpathElems = ((Thread.currentThread().contextClassLoader) as URLClassLoader).urLs
                    .map { Paths.get(it.toURI()).toFile() }

            return KotlinJsr223JvmLocalScriptEngine(Disposer.newDisposable(), this,
                    classpathElems,
                    KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
                    { ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
                    arrayOf(Bindings::class))
        }
    }

    private val engine: KotlinJsr223JvmLocalScriptEngine

    init {
        engine = EngineFactory().scriptEngine as KotlinJsr223JvmLocalScriptEngine
    }

    override fun eval(script: String): Any? {
        val context = SimpleScriptContext()
        val r = engine.eval(script, context)
        engine.state.history.reset()
        return r
    }

    override fun eval(script: Reader): Any? {
        val context = SimpleScriptContext()
        val r = engine.eval(script, context)
        engine.state.history.reset()
        return r
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

    override fun loadScript(file: Path): Any? {
        InputStreamReader(Files.newInputStream(file)).use {
            return eval(it)
        }
    }

    override fun loadScriptFromClasspath(file: String): Any? {
        InputStreamReader(javaClass.getResourceAsStream("/" + file)).use {
            return eval(it)
        }
    }
}