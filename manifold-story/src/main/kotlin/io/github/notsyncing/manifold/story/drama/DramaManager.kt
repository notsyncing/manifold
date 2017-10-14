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
import java.util.concurrent.Executors
import java.util.logging.Logger
import javax.script.*
import kotlin.concurrent.thread

object DramaManager {
    private const val DRAMA_EXT = ".drama.js"

    private lateinit var engine: ScriptEngine
    private val engineCompilable get() = engine as Compilable
    private val engineInvocable get() = engine as Invocable

    private val dramaSearchPaths = mutableListOf("$")

    var ignoreClasspathDramas = false

    private lateinit var dramaWatcher: WatchService
    private lateinit var dramaWatchingThread: Thread
    private val dramaWatchingPaths = mutableMapOf<WatchKey, Path>()
    private var dramaWatching = true

    private var isInit = true

    private val actionMap = ConcurrentHashMap<String, DramaActionInfo>()

    private val dramaReloadThread = Executors.newSingleThreadExecutor {
        Thread(it).apply {
            this.name = "manifold.drama.reloader"
            this.isDaemon = true
        }
    }

    private val logger = Logger.getLogger(javaClass.simpleName)

    fun addDramaSearchPath(p: Path) {
        addDramaSearchPath(p.toAbsolutePath().normalize().toString())
    }

    fun addDramaSearchPath(p: String) {
        if (dramaSearchPaths.contains(p)) {
            logger.warning("Drama search paths already contain $p, will skip adding it.")
            return
        }

        dramaSearchPaths.add(p)

        if (!isInit) {
            loadAndWatchDramasFromDirectory(p)
        }
    }

    fun init() {
        isInit = true

        engine = ScriptEngineManager().getEngineByName("nashorn")

        engine.eval("load('classpath:manifold_story/drama-lib.js')")

        dramaWatcher = FileSystems.getDefault().newWatchService()

        dramaWatching = true
        dramaWatchingThread = thread(name = "manifold.drama.watcher", isDaemon = true, block = this::dramaWatcherThread)

        loadAllDramas()

        ManifoldDomain.onClose { domain, _ ->
            actionMap.removeIf { (_, info) -> info.domain == domain.name }
        }

        isInit = false
    }

    private fun evalDrama(reader: Reader, domain: String?, path: String) {
        reader.use {
            engine.context.setAttribute("__MANIFOLD_DRAMA_CURRENT_FILE__", path, ScriptContext.ENGINE_SCOPE)
            engine.context.setAttribute("__MANIFOLD_DRAMA_CURRENT_DOMAIN__", domain, ScriptContext.ENGINE_SCOPE)

            if ((domain == null) || (domain == ManifoldDomain.ROOT)) {
                engine.eval("load('classpath:$path')")
            } else {
                engine.eval(it)
            }

            engine.context.removeAttribute("__MANIFOLD_DRAMA_CURRENT_FILE__", ScriptContext.ENGINE_SCOPE)
            engine.context.removeAttribute("__MANIFOLD_DRAMA_CURRENT_DOMAIN__", ScriptContext.ENGINE_SCOPE)
        }
    }

    private fun evalDrama(inputStream: InputStream, domain: String?, path: String) {
        evalDrama(InputStreamReader(inputStream), domain, path)
    }

    private fun evalDrama(file: Path, domain: String?) {
        val path = file.toAbsolutePath().normalize().toString()

        engine.context.setAttribute("__MANIFOLD_DRAMA_CURRENT_FILE__", path, ScriptContext.ENGINE_SCOPE)
        engine.context.setAttribute("__MANIFOLD_DRAMA_CURRENT_DOMAIN__", domain, ScriptContext.ENGINE_SCOPE)

        engine.eval("load('${path}')")

        engine.context.removeAttribute("__MANIFOLD_DRAMA_CURRENT_FILE__", ScriptContext.ENGINE_SCOPE)
        engine.context.removeAttribute("__MANIFOLD_DRAMA_CURRENT_DOMAIN__", ScriptContext.ENGINE_SCOPE)
    }

    private fun loadAndWatchDramasFromDirectory(path: String) {
        val p = Paths.get(path)

        Files.list(p)
                .filter { it.fileName.toString().endsWith(DRAMA_EXT) }
                .forEach { f ->
                    logger.info("Loading drama $f")

                    evalDrama(f, null)
                }

        val watchKey = p.register(dramaWatcher, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY)
        dramaWatchingPaths[watchKey] = p
    }

    private fun loadAllDramas() {
        dramaWatchingPaths.keys.forEach { it.cancel() }
        dramaWatchingPaths.clear()

        actionMap.clear()

        for (path in dramaSearchPaths) {
            if (path == "$") {
                if (ignoreClasspathDramas) {
                    continue
                }

                Manifold.dependencyProvider?.getAllClasspathFilesWithDomain()
                        ?.filter { (_, _, relPath) -> relPath.endsWith(DRAMA_EXT) }
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
                loadAndWatchDramasFromDirectory(path)
            }
        }
    }

    fun removeActionIf(predicate: (DramaActionInfo) -> Boolean) {
        actionMap.removeIf { (_, info) -> predicate(info) }
    }

    @JvmStatic
    fun registerAction(name: String, permissionName: String?, permissionType: String?, code: ScriptObjectMirror,
                       fromPath: String, domain: String?) {
        var realName = name

        if ((!domain.isNullOrBlank()) && (domain != ManifoldDomain.ROOT)) {
            realName = "${domain}_$name"
        }

        if (actionMap.containsKey(realName)) {
            logger.warning("Action map already contains an action named $name ($realName), the previous one " +
                    "will be overwritten!")
        }

        val actionInfo = DramaActionInfo(name, permissionName, permissionType, code, fromPath, domain)
        actionMap[realName] = actionInfo
    }

    fun destroy() {
        dramaWatching = false
        dramaWatcher.close()

        dramaWatchingThread.interrupt()
    }

    fun reset() {
        dramaSearchPaths.clear()
        dramaSearchPaths.add("$")
        dramaWatchingPaths.keys.forEach { it.cancel() }
        dramaWatchingPaths.clear()

        actionMap.clear()

        ignoreClasspathDramas = false
    }

    private fun updateDramaActions(dramaFile: Path, type: WatchEvent.Kind<*>) {
        val iter = actionMap.iterator()
        val dramaFilePathStr = dramaFile.toString()

        if ((type == StandardWatchEventKinds.ENTRY_MODIFY) || (type == StandardWatchEventKinds.ENTRY_DELETE)) {
            while (iter.hasNext()) {
                val (_, info) = iter.next()

                if (Files.isDirectory(dramaFile)) {
                    if (Paths.get(info.fromPath).startsWith(dramaFile)) {
                        iter.remove()
                    }
                } else {
                    if (info.fromPath == dramaFilePathStr) {
                        iter.remove()
                    }
                }

                Manifold.hooks.removeFromCacheIf { it.source?.startsWith(dramaFilePathStr + "?") ?: false }
            }
        }

        if (type != StandardWatchEventKinds.ENTRY_DELETE) {
            evalDrama(dramaFile, null)
        }

        logger.info("Drama file $dramaFile updated, type $type.")
    }

    private fun dramaWatcherThread() {
        while (dramaWatching) {
            val key: WatchKey

            try {
                key = dramaWatcher.take()
            } catch (x: InterruptedException) {
                continue
            } catch (x: ClosedWatchServiceException) {
                if (!dramaWatching) {
                    continue
                }

                throw x
            }

            for (event in key.pollEvents()) {
                val kind = event.kind()

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    logger.warning("Drama watcher overflow, at ${dramaWatchingPaths[key]}")
                    continue
                }

                val ev = event as WatchEvent<Path>
                val filename = ev.context()

                if (!filename.toString().endsWith(DRAMA_EXT)) {
                    continue
                }

                val dir = dramaWatchingPaths[key]

                if (dir == null) {
                    logger.warning("We are notified by a path not in the watching list, filename: $filename")
                    continue
                }

                try {
                    val fullPath = dir.resolve(filename)

                    dramaReloadThread.submit {
                        try {
                            updateDramaActions(fullPath, kind)
                        } catch (e: Exception) {
                            logger.warning("An exception occured when reloading drama $fullPath: ${e.message}")
                            e.printStackTrace()
                        }
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