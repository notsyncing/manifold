package io.github.notsyncing.manifold.story.drama

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.domain.ManifoldDomain
import io.github.notsyncing.manifold.utils.removeIf
import jdk.nashorn.api.scripting.ScriptObjectMirror
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI
import java.nio.file.*
import java.security.InvalidParameterException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.LogManager
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

    val dramaSearchPaths = mutableListOf("$")

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

    fun init() {
        loadAllDramas()

        ManifoldDomain.onClose { domain, _ ->
            actionMap.removeIf { (_, info) -> info.domain == domain.name }
        }
    }

    private fun evalDrama(reader: Reader, domain: String?, path: String) {
        reader.use {
            engine.context.setAttribute("__MANIFOLD_DRAMA_CURRENT_FILE__", path, ScriptContext.ENGINE_SCOPE)
            engine.context.setAttribute("__MANIFOLD_DRAMA_CURRENT_DOMAIN__", domain, ScriptContext.ENGINE_SCOPE)
            engine.eval(it)
        }
    }

    private fun evalDrama(inputStream: InputStream, domain: String?, path: String) {
        evalDrama(InputStreamReader(inputStream), domain, path)
    }

    private fun loadAllDramas() {
        dramaWatchingPaths.keys.forEach { it.cancel() }
        dramaWatchingPaths.clear()

        actionMap.clear()

        for (path in dramaSearchPaths) {
            if (path == "$") {
                Manifold.dependencyProvider?.getAllClasspathFilesWithDomain()
                        ?.filter { (_, _, relPath) -> relPath.endsWith(".drama.js") }
                        ?.forEach { (domain, containingPath, relPath) ->
                            val fullPath: Path
                            val fileName = containingPath.fileName.toString()
                            var fs: FileSystem? = null

                            try {
                                if (Files.isDirectory(containingPath)) {
                                    fullPath = containingPath.resolve(relPath)
                                } else if ((fileName.endsWith(".jar")) || (fileName.endsWith(".war"))) {
                                    val uri = containingPath.toUri()
                                    val zipUri = URI("jar:" + uri.scheme, uri.path, null)

                                    fs = FileSystems.newFileSystem(zipUri, mutableMapOf<String, String>())
                                    fullPath = fs.getPath(relPath)
                                } else {
                                    logger.warning("Unsupported element $containingPath $relPath on classpath, skipping.")
                                    return@forEach
                                }

                                Files.newBufferedReader(fullPath).use {
                                    logger.info("Loading drama $relPath from classpath")
                                    evalDrama(it, domain.name, relPath)
                                }
                            } finally {
                                if (fs != null) {
                                    fs.close()
                                }
                            }
                        }
            } else {
                val p = Paths.get(path)

                Files.list(p)
                        .forEach { f ->
                            logger.info("Loading drama $f")

                            Files.newBufferedReader(f).use {
                                evalDrama(it, null, f.toString())
                            }
                        }

                val watchKey = p.register(dramaWatcher, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY)
                dramaWatchingPaths[watchKey] = p
            }
        }
    }

    fun removeActionIf(predicate: (DramaActionInfo) -> Boolean) {
        actionMap.removeIf { (_, info) -> predicate(info) }
    }

    @JvmStatic
    fun registerAction(name: String, permissionName: String, permissionType: String, code: ScriptObjectMirror,
                       fromPath: String, domain: String?) {
        if (actionMap.containsKey(name)) {
            logger.warning("Action map already contains an action named $name, the previous one " +
                    "will be overwritten!")
        }

        var realName = name

        if (!domain.isNullOrBlank()) {
            realName = "${domain}_$name"
        }

        val actionInfo = DramaActionInfo(name, permissionName, permissionType, code, fromPath, domain)
        actionMap[realName] = actionInfo
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

    private fun updateDramaActions(dramaFile: Path, type: WatchEvent.Kind<*>) {
        val iter = actionMap.iterator()
        val dramaFileStr = dramaFile.toString()

        if ((type == StandardWatchEventKinds.ENTRY_MODIFY) || (type == StandardWatchEventKinds.ENTRY_DELETE)) {
            while (iter.hasNext()) {
                val (_, info) = iter.next()

                if (Files.isDirectory(dramaFile)) {
                    if (Paths.get(info.fromPath).startsWith(dramaFile)) {
                        iter.remove()
                    }
                } else {
                    if (info.fromPath == dramaFileStr) {
                        iter.remove()
                    }
                }
            }
        }

        if (type != StandardWatchEventKinds.ENTRY_DELETE) {
            Files.newBufferedReader(dramaFile).use {
                engine.eval(it)
            }
        }

        logger.info("Drama file $dramaFile updated, type $type.")
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

                if (kind == StandardWatchEventKinds.OVERFLOW) {
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
                        updateDramaActions(fullPath, kind)
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

    fun getAction(name: String, domain: String? = null) = actionMap[if (domain.isNullOrBlank()) name else "${domain}_$name"]

    fun perform(scene: DramaScene, actionInfo: DramaActionInfo, parameters: JSONObject,
                permissionParameters: JSONObject?) = future<Any?> {
        val functor = actionInfo.code

        if (!functor.isFunction) {
            throw InvalidParameterException("Drama action ${actionInfo.name} does not contain a function!")
        }

        val context = DramaActionContext(engine, scene, actionInfo.domain)
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