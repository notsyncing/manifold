package io.github.notsyncing.manifold.story.drama

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldActionContextRunner
import io.github.notsyncing.manifold.domain.ManifoldDomain
import io.github.notsyncing.manifold.story.drama.engine.CallableObject
import io.github.notsyncing.manifold.story.drama.engine.DramaEngineFactory
import io.github.notsyncing.manifold.utils.removeIf
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI
import java.nio.file.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread

object DramaManager {
    private val dramaSearchPaths = mutableListOf("$")

    var ignoreClasspathDramas = false

    private lateinit var dramaWatcher: WatchService
    private lateinit var dramaWatchingThread: Thread
    private val dramaWatchingPaths = mutableMapOf<WatchKey, Path>()
    private var dramaWatching = true

    private var isInit = true

    private val actionMap = ConcurrentHashMap<String, DramaActionInfo>()
    private val scriptLifecycleMap = ConcurrentHashMap<String, DramaScriptLifecycle>()

    private val dramaReloadThread = Executors.newSingleThreadExecutor {
        Thread(it).apply {
            this.name = "manifold.drama.reloader"
            this.isDaemon = true
        }
    }

    var currentFile: String? = null

    private val logger = Logger.getLogger(javaClass.simpleName)

    fun addDramaSearchPath(p: Path) {
        addDramaSearchPath(p.toAbsolutePath().normalize().toString())
    }

    fun addDramaSearchPath(p: String) {
        if (dramaSearchPaths.contains(p)) {
            logger.warning("Drama search paths already contain $p, will skip adding it.")
            return
        }

        if (!Files.isDirectory(Paths.get(p))) {
            logger.warning("Drama search path $p does not exist or not a directory, will skip adding it.")
            return
        }

        dramaSearchPaths.add(p)

        if (!isInit) {
            loadAndWatchDramasFromDirectory(p)
        }
    }

    fun init() {
        isInit = true

        dramaWatcher = FileSystems.getDefault().newWatchService()

        dramaWatching = true
        dramaWatchingThread = thread(name = "manifold.drama.watcher", isDaemon = true, block = this::dramaWatcherThread)

        loadAllDramas()

        ManifoldDomain.onClose { domain, _ ->
            actionMap.removeIf { (_, info) -> info.domain == domain.name }

            val iter = scriptLifecycleMap.iterator()

            while (iter.hasNext()) {
                val (_, listener) = iter.next()

                if (listener.javaClass.classLoader == domain.classLoader) {
                    listener.beforeDestroy()

                    iter.remove()
                }
            }
        }

        isInit = false
    }

    private fun evalDrama(reader: Reader, domain: String?, path: String) {
        currentFile = path
        val engine = DramaEngineFactory.getByFile(path)

        reader.use {
            engine.setContextAttribute("__MANIFOLD_DRAMA_CURRENT_FILE__", path)
            engine.setContextAttribute("__MANIFOLD_DRAMA_CURRENT_DOMAIN__", domain)

            val r = if ((domain == null) || (domain == ManifoldDomain.ROOT)) {
                engine.loadScriptFromClasspath(path)
            } else {
                engine.eval(it)
            }

            if (r is DramaScriptLifecycle) {
                scriptLifecycleMap[path] = r

                r.afterEvaluate()
            }

            engine.removeContextAttribute("__MANIFOLD_DRAMA_CURRENT_FILE__")
            engine.removeContextAttribute("__MANIFOLD_DRAMA_CURRENT_DOMAIN__")
        }

        currentFile = null
    }

    private fun evalDrama(inputStream: InputStream, domain: String?, path: String) {
        evalDrama(InputStreamReader(inputStream), domain, path)
    }

    private fun evalDrama(file: Path, domain: String?) {
        val path = file.toAbsolutePath().normalize().toString()
        currentFile = path
        val engine = DramaEngineFactory.getByFile(path)

        engine.setContextAttribute("__MANIFOLD_DRAMA_CURRENT_FILE__", path)
        engine.setContextAttribute("__MANIFOLD_DRAMA_CURRENT_DOMAIN__", domain)

        val r = engine.loadScript(file)

        if (r is DramaScriptLifecycle) {
            scriptLifecycleMap[path] = r

            r.afterEvaluate()
        }

        engine.removeContextAttribute("__MANIFOLD_DRAMA_CURRENT_FILE__")
        engine.removeContextAttribute("__MANIFOLD_DRAMA_CURRENT_DOMAIN__")

        currentFile = null
    }

    private fun loadAndWatchDramasFromDirectory(path: String) {
        val p = Paths.get(path)
        var counter = 0

        Files.list(p)
                .filter { DramaEngineFactory.isSupportedFile(it.fileName.toString()) }
                .forEach { f ->
                    logger.info("Loading drama $f")

                    try {
                        evalDrama(f, null)

                        counter++
                    } catch (e: Exception) {
                        logger.log(Level.WARNING, "Failed to load drama $f", e)
                    }
                }

        val watchKey = p.register(dramaWatcher, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY)
        dramaWatchingPaths[watchKey] = p

        logger.info("Loaded $counter dramas from $path and added it to the watching list.")
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
                        ?.filter { (_, _, relPath) -> DramaEngineFactory.isSupportedFile(relPath) }
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

                                    try {
                                        evalDrama(it, domain.name, relPath)
                                    } catch (e: Exception) {
                                        logger.log(Level.WARNING, "Failed to load drama $relPath", e)
                                    }
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
    fun registerAction(name: String, isInternal: Boolean, permissionName: String?, permissionType: String?,
                       code: CallableObject, fromPath: String, domain: String?) {
        var realName = name

        if ((!domain.isNullOrBlank()) && (domain != ManifoldDomain.ROOT)) {
            realName = "${domain}_$name"
        }

        if (actionMap.containsKey(realName)) {
            logger.warning("Action map already contains an action named $name ($realName), the previous one " +
                    "will be overwritten!")
        }

        val actionInfo = DramaActionInfo(name, permissionName, permissionType, code, fromPath, domain, isInternal)
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
        val dramaFilePathStr = dramaFile.toString()

        if ((type == StandardWatchEventKinds.ENTRY_MODIFY) || (type == StandardWatchEventKinds.ENTRY_DELETE)) {
            val iter = actionMap.iterator()

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

            val iter2 = scriptLifecycleMap.iterator()

            while (iter2.hasNext()) {
                val (path, listener) = iter2.next()

                if (Files.isDirectory(dramaFile)) {
                    if (Paths.get(path).startsWith(dramaFile)) {
                        listener.beforeDestroy()
                        iter.remove()
                    }
                } else {
                    if (path == dramaFilePathStr) {
                        listener.beforeDestroy()
                        iter.remove()
                    }
                }
            }
        }

        try {
            logger.info("Drama file $dramaFile updated, type $type.")

            if (type != StandardWatchEventKinds.ENTRY_DELETE) {
                logger.info("Reloading drama file $dramaFile")

                evalDrama(dramaFile, null)

                logger.info("Drama file $dramaFile reloaded.")
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to update drama $dramaFile", e)
        }
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

                if (!DramaEngineFactory.isSupportedFile(filename.toString())) {
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
                permissionParameters: JSONObject?) = future {
        val functor = actionInfo.code

        val engine = DramaEngineFactory.getByFile(actionInfo.fromPath)
        val context = DramaActionContext(engine, scene, actionInfo.domain)

        try {
            val result = functor.call(null, context, parameters.toJSONString(), permissionParameters?.toJSONString())

            val realResult = if (result is CompletableFuture<*>) {
                result.await()
            } else {
                result
            }

            realResult
        } finally {
            context.destroy().await()
        }
    }

    fun perform(actionName: String, domain: String? = null, parameters: JSONObject,
                permissionParameters: JSONObject?): CompletableFuture<Any?> {
        val actionInfo = getAction(actionName, domain)

        if (actionInfo == null) {
            throw ClassNotFoundException("No action with name $actionName " +
                    "${if (domain != null) "in domain $domain" else ""} found!")
        }

        val scene = DramaScene(actionName, domain, parameters)
        scene.m = ManifoldActionContextRunner(null, scene.context)

        return perform(scene, actionInfo, parameters, permissionParameters)
    }
}