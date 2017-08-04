package io.github.notsyncing.manifold.story.drama

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.Manifold
import jdk.nashorn.api.scripting.ScriptObjectMirror
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.file.*
import java.security.InvalidParameterException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import javax.script.Compilable
import javax.script.Invocable
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import kotlin.concurrent.thread

object DramaManager {
    private val engine = ScriptEngineManager().getEngineByName("nashorn")
    private val engineCompilable = engine as Compilable
    private val engineInvocable = engine as Invocable

    val dramaSearchPaths = mutableListOf<String>()

    private val dramaWatcher = FileSystems.getDefault().newWatchService()
    private val dramaWatchingPaths = mutableMapOf<WatchKey, Path>()
    private val dramaWatchingThread = thread(isDaemon = true, block = this::dramaWatcherThread)
    private var dramaWatching = true

    private val actionMap = ConcurrentHashMap<String, DramaActionInfo>()

    private val logger = Logger.getLogger(javaClass.simpleName)

    init {
        engine.eval("load('classpath:net/arnx/nashorn/lib/promise.js')")
        engine.eval("load('classpath:manifold_story/drama-lib.js')")
    }

    private fun evalDrama(reader: Reader, path: String) {
        reader.use {
            engine.context.setAttribute("__MANIFOLD_DRAMA_CURRENT_FILE__", path, ScriptContext.ENGINE_SCOPE)
            engine.eval(it)
        }
    }

    private fun evalDrama(inputStream: InputStream, path: String) {
        evalDrama(InputStreamReader(inputStream), path)
    }

    fun loadAllDramas() {
        dramaWatchingPaths.keys.forEach { it.cancel() }
        dramaWatchingPaths.clear()

        for (path in dramaSearchPaths) {
            if (path == "$") {
                Manifold.dependencyProvider?.getAllClasspathFiles()
                        ?.filter { it.endsWith(".drama.js") }
                        ?.forEach { f ->
                            DramaScene::class.java.getResourceAsStream(f)
                                    .use {
                                        evalDrama(it, f)
                                    }
                        }
            } else {
                val p = Paths.get(path)

                Files.list(p)
                        .forEach { f ->
                            logger.fine("Loading drama $f")

                            Files.newBufferedReader(f).use {
                                evalDrama(it, f.toString())
                            }
                        }

                val watchKey = p.register(dramaWatcher, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY)
                dramaWatchingPaths[watchKey] = p
            }
        }

        actionMap.clear()
    }

    @JvmStatic
    fun registerAction(name: String, permissionName: String, permissionType: String, code: String, fromPath: String) {
        if (actionMap.containsKey(name)) {
            logger.warning("Action map already contains an action named $name, the previous one " +
                    "will be overwritten!")
        }

        val compiledCode = engineCompilable.compile(code)

        val actionInfo = DramaActionInfo(name, permissionName, permissionType, compiledCode, fromPath)
        actionMap[name] = actionInfo
    }

    fun destroy() {
        dramaWatching = false
        dramaWatcher.close()
    }

    fun reset() {
        dramaSearchPaths.clear()
        dramaWatchingPaths.keys.forEach { it.cancel() }
        dramaWatchingPaths.clear()
        actionMap.clear()
    }

    private fun updateDramaActions(dramaFile: Path) {
        val iter = actionMap.iterator()
        val dramaFileStr = dramaFile.toString()

        while (iter.hasNext()) {
            val (_, info) = iter.next()

            if (info.fromPath == dramaFileStr) {
                iter.remove()
            }
        }

        Files.newBufferedReader(dramaFile).use {
            engine.eval(it)
        }

        logger.fine("Drama file $dramaFile updated.")
    }

    private fun dramaWatcherThread() {
        while (dramaWatching) {
            val key: WatchKey

            try {
                key = dramaWatcher.take()
            } catch (x: InterruptedException) {
                return
            }

            for (event in key.pollEvents()) {
                val kind = event.kind()

                if (kind === StandardWatchEventKinds.OVERFLOW) {
                    logger.warning("Drama watcher overflow, at ${dramaWatchingPaths[key]}")
                    continue
                }

                val ev = event as WatchEvent<Path>
                val filename = ev.context()
                val dir = dramaWatchingPaths[key]

                if (dir == null) {
                    logger.warning("We are notified by a path not in the watching list, filename: $filename")
                    continue
                }

                try {
                    val fullPath = dir.resolve(filename)

                    CompletableFuture.runAsync {
                        updateDramaActions(fullPath)
                    }
                } catch (x: IOException) {
                    x.printStackTrace()
                    continue
                }
            }

            val valid = key.reset()

            if (!valid) {
                continue
            }
        }
    }

    fun getAction(name: String) = actionMap[name]

    fun perform(scene: DramaScene, actionInfo: DramaActionInfo, parameters: JSONObject,
                permissionParameters: JSONObject?) = future<Any?> {
        val bindings = engine.createBindings()
        val functor = actionInfo.code.eval(bindings)

        if (functor !is ScriptObjectMirror) {
            throw InvalidParameterException("Drama action ${actionInfo.name} has invalid code type $functor")
        }

        if (!functor.isFunction) {
            throw InvalidParameterException("Drama action ${actionInfo.name} does not contain a function!")
        }

        val context = DramaActionContext(engine, scene)
        val result = functor.call(null, context, parameters.toJSONString(), permissionParameters?.toJSONString())

        val realResult = if (result is CompletableFuture<*>) {
            result.await()
        } else {
            result
        }

        context.destroy().await()

        realResult
    }
}