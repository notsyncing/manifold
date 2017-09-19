package io.github.notsyncing.manifold.domain

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.lukehutch.fastclasspathscanner.scanner.ClassInfo
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult
import io.github.lukehutch.fastclasspathscanner.scanner.ScanSpec
import io.github.notsyncing.manifold.utils.FileUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.URL
import java.nio.file.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class ManifoldDomain(val name: String = ROOT,
                     val parentDomain: ManifoldDomain? = null) : AutoCloseable {
    companion object {
        const val ROOT = "root"

        private val afterScanHandlers = mutableListOf<(ManifoldDomain) -> Unit>()
        private val onCloseHandlers = mutableListOf<(ManifoldDomain, DomainClassLoader) -> Unit>()

        fun afterScan(handler: (ManifoldDomain) -> Unit) {
            afterScanHandlers.add(handler)
        }

        fun onClose(handler: (ManifoldDomain, DomainClassLoader) -> Unit) {
            onCloseHandlers.add(handler)
        }
    }

    private val childDomains = mutableListOf<ManifoldDomain>()
    private val urls = mutableListOf<URL>()
    private val urlsFromWars = mutableMapOf<URL, URL>()
    private var classLoader: DomainClassLoader = createClassLoader()
    private val classpathWatcher = FileSystems.getDefault().newWatchService()
    private val classpathWatcherKeys = ConcurrentHashMap<WatchKey, Path>()
    private var closed = false
    private var scanning = false
    private var needRescan = false
    private val tempDir = Files.createTempDirectory("manifold-domain-")
    private var onClose: () -> CompletableFuture<Unit> = { CompletableFuture.completedFuture(Unit) }

    private lateinit var scanner: FastClasspathScanner
    private var classScanResult: ScanResultWrapper? = null
    private val fileScanResult = mutableListOf<Triple<ManifoldDomain, Path, String>>()

    init {
        if (parentDomain != null) {
            parentDomain.childDomains.add(this)
        }

        thread(isDaemon = true, block = this::classpathWatcherThread)
    }

    fun onClose(handler: () -> CompletableFuture<Unit>) {
        this.onClose = handler
    }

    private fun classpathWatcherThread() {
        while (!closed) {
            try {
                val key = classpathWatcher.take()
                key.reset()

                for (event in key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue
                    }

                    val file = event.context() as Path
                    val fileStr = file.toString()

                    if ((!fileStr.endsWith(".jar")) && (!fileStr.endsWith(".war"))) {
                        continue
                    }

                    val dir = classpathWatcherKeys[key]

                    if (dir == null) {
                        throw IOException("No matching directory for key $key: ${event.kind()} $file")
                    }

                    val fullPath = dir.resolve(file)
                    val url = fullPath.toUri().toURL()

                    if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                        urls.remove(url)
                    } else {
                        if ((event.kind() == StandardWatchEventKinds.ENTRY_CREATE) && (!fileStr.endsWith(".war"))) {
                            urls.add(url)
                        }

                        if (file.toString().endsWith(".war")) {
                            CompletableFuture.runAsync {
                                scanning = true

                                val warDir = tempDir.resolve(file)

                                FileUtils.deleteRecursive(warDir)
                                addWar(fullPath)

                                closeClassLoader()
                                classLoader = createClassLoader()

                                scanClasspath()
                            }
                        }
                    }
                }

                if (scanning) {
                    needRescan = true
                    continue
                } else {
                    CompletableFuture.runAsync {
                        closeClassLoader()
                        classLoader = createClassLoader()

                        scanClasspath()
                    }
                }
            } catch (e: Exception) {
                if (e is ClosedWatchServiceException) {
                    if (closed) {
                        continue
                    }
                }

                e.printStackTrace()
            }
        }
    }

    fun init() {
        reset()
    }

    fun createSubDomain(name: String) = ManifoldDomain(name, this)

    fun createClassLoader() = DomainClassLoader(name, urls.toTypedArray(),
            parentDomain?.classLoader ?: javaClass.classLoader)

    private fun createScanner() = FastClasspathScanner("-com.github.mauricio", "-scala")

    fun inAllClassScanResults(handler: (ScanResultWrapper?, ClassLoader) -> Unit) {
        handler(classScanResult, classLoader)

        childDomains.forEach { it.inAllClassScanResults(handler) }
    }

    fun inCurrentClassScanResult(handler: (ScanResultWrapper?, ClassLoader) -> Unit) {
        handler(classScanResult, classLoader)
    }

    fun inAllFileScanResults(handler: (List<Triple<ManifoldDomain, Path, String>>, ClassLoader) -> Unit) {
        handler(fileScanResult, classLoader)

        childDomains.forEach { it.inAllFileScanResults(handler) }
    }

    fun inCurrentFileScanResult(handler: (List<Triple<ManifoldDomain, Path, String>>, ClassLoader) -> Unit) {
        handler(fileScanResult, classLoader)
    }

    // Dark black magic lives here...
    private fun filterScanResult(result: ScanResult): ScanResultWrapper {
        if (this.name == ROOT) {
            return ScanResultWrapper(result, emptySet(), classLoader)
        }

        val classGraphBuilderField = result.javaClass.getDeclaredField("classGraphBuilder")
                .apply { this.isAccessible = true }
        val classGraphBuilder = classGraphBuilderField.get(result)

        val classNameToClassLoadersField = classGraphBuilder.javaClass.getDeclaredField("classNameToClassLoaders")
                .apply { this.isAccessible = true }
        val classNameToClassLoaders = classNameToClassLoadersField.get(classGraphBuilder) as MutableMap<String, List<ClassLoader>>

        val classesToRemove = classNameToClassLoaders.filter { (_, classloaders) -> !classloaders.contains(classLoader) }
                .map { (className, _) -> className }
                .toHashSet()

        return ScanResultWrapper(result, classesToRemove, classLoader)
    }

    private fun scanClasspath() {
        scanning = true
        needRescan = false

        fileScanResult.clear()
        classScanResult = null

        if ((this.name != ROOT) && (urls.isEmpty())) {
            return
        }

        classScanResult = filterScanResult(scanner
                .apply {
                    if (name != ROOT) {
                        this.overrideClassLoaders(classLoader)
                    }
                }
                .matchFilenamePattern(".*") { classpathElem: File?, relativePath: String?, _: InputStream?, _: Long ->
                    if (classLoader.domainName != ROOT) {
                        if ((classpathElem == null) || (!urls.contains(classpathElem.toURI().toURL()))) {
                            return@matchFilenamePattern
                        }
                    }

                    if ((classpathElem != null) && (relativePath != null)) {
                        fileScanResult.add(Triple(this, classpathElem.toPath(), relativePath))
                    }
                }
                .scan())

        afterScanHandlers.forEach { it(this) }

        scanning = false

        if (needRescan) {
            scanClasspath()
        }
    }

    fun reset() {
        childDomains.forEach { it.close() }

        if (this.name != ROOT) {
            classLoader.close()
            classLoader = createClassLoader()
        }

        scanner = createScanner()
        scanClasspath()
    }

    private fun addWar(path: Path) {
        val dir = tempDir.resolve(path.fileName.toString())
        Files.createDirectories(dir)

        FileUtils.unzip(path, dir)

        val warClasses = dir.resolve("WEB-INF/classes")
        val warLibs = dir.resolve("WEB-INF/lib")

        if (Files.isDirectory(warClasses)) {
            urls.add(warClasses.toUri().toURL())
        }

        if (Files.isDirectory(warLibs)) {
            Files.list(warLibs)
                    .filter { it.fileName.toString().endsWith(".jar") }
                    .forEach {
                        val url = it.toUri().toURL()
                        urls.add(url)
                        urlsFromWars[url] = path.toUri().toURL()
                    }
        }
    }

    fun addClasspath(vararg urls: URL) {
        urls.forEach {
            if (it.protocol == "file") {
                val path = Paths.get(it.toURI())

                if (path.fileName.toString().endsWith(".war")) {
                    addWar(path)
                } else {
                    this.urls.add(it)
                }

                if (Files.isDirectory(path)) {
                    if (classpathWatcherKeys.containsValue(path)) {
                        return@forEach
                    }

                    val key = path.register(classpathWatcher, StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)
                    classpathWatcherKeys[key] = path
                } else {
                    if (classpathWatcherKeys.containsValue(path.parent)) {
                        return@forEach
                    }

                    val key = path.parent.register(classpathWatcher, StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)

                    classpathWatcherKeys[key] = path.parent
                }
            }
        }

        closeClassLoader()
        classLoader = createClassLoader()

        scanClasspath()
    }

    fun addClasspath(vararg files: Path) {
        addClasspath(*files.map { it.toUri().toURL() }.toTypedArray())
    }

    fun removeClasspath(vararg urls: URL) {
        urls.forEach {
            this.urls.remove(it)

            val iter = urlsFromWars.iterator()

            while (iter.hasNext()) {
                val (fileUrl, warUrl) = iter.next()

                if (warUrl == it) {
                    this.urls.remove(fileUrl)
                }
            }
        }

        closeClassLoader()
        classLoader = createClassLoader()

        scanClasspath()
    }

    fun removeClasspath(vararg files: Path) {
        removeClasspath(*files.map { it.toUri().toURL() }.toTypedArray())
    }

    fun clearAndAddClasspath(vararg urls: URL) {
        this.urls.clear()
        this.urlsFromWars.clear()

        classpathWatcherKeys.forEach { key, _ -> key.cancel() }
        classpathWatcherKeys.clear()

        FileUtils.deleteRecursive(tempDir)

        addClasspath(*urls)
    }

    fun clearAndAddClasspath(vararg files: Path) {
        clearAndAddClasspath(*files.map { it.toUri().toURL() }.toTypedArray())
    }

    fun closeClassLoader() {
        onCloseHandlers.forEach { it(this, classLoader) }

        classLoader.close()
    }

    override fun close() {
        closed = true
        classpathWatcherKeys.forEach { key, _ -> key.cancel() }
        classpathWatcherKeys.clear()
        classpathWatcher.close()

        onClose().thenAccept {
            FileUtils.deleteRecursive(tempDir)

            if (parentDomain != null) {
                parentDomain.childDomains.remove(this)
            }

            onCloseHandlers.forEach { it(this, classLoader) }

            childDomains.forEach { it.close() }

            classLoader.close()
        }
    }

    fun findDomain(domainName: String): WeakReference<ManifoldDomain>? {
        if (name == domainName) {
            return WeakReference(this)
        }

        for (c in childDomains) {
            val d = c.findDomain(domainName)

            if (d != null) {
                return d
            }
        }

        return null
    }

    fun loadClass(className: String): Class<*>? {
        return classLoader.loadClass(className)
    }
}