package io.github.notsyncing.manifold.domain

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult
import io.github.notsyncing.manifold.utils.FileUtils
import java.io.IOException
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
    private var classLoader: DomainClassLoader = createClassLoader()
    private val classpathWatcher = FileSystems.getDefault().newWatchService()
    private val classpathWatcherKeys = ConcurrentHashMap<WatchKey, Path>()
    private var closed = false
    private var scanning = false
    private var needRescan = false
    private val tempDir = Files.createTempDirectory("manifold-domain-")

    private lateinit var scanner: FastClasspathScanner
    private lateinit var classScanResult: ScanResult

    init {
        if (parentDomain != null) {
            parentDomain.childDomains.add(this)
        }

        thread(isDaemon = true, block = this::classpathWatcherThread)
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

                    val dir = classpathWatcherKeys[key]

                    if (dir == null) {
                        throw IOException("No matching directory for key $key: ${event.kind()} $file")
                    }

                    val fullPath = dir.resolve(file)

                    if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                        urls.remove(fullPath.toUri().toURL())
                    } else {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            urls.add(fullPath.toUri().toURL())
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
            .apply {
                if (parentDomain != null) {
                    this.overrideClassLoaders(classLoader)
                }
            }

    fun inAllClassScanResults(handler: (ScanResult, ClassLoader) -> Unit) {
        handler(classScanResult, classLoader)

        childDomains.forEach { it.inAllClassScanResults(handler) }
    }

    fun inCurrentClassScanResult(handler: (ScanResult, ClassLoader) -> Unit) {
        handler(classScanResult, classLoader)
    }

    private fun scanClasspath() {
        scanning = true
        needRescan = false

        classScanResult = scanner.scan()

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
                    .forEach { urls.add(it.toUri().toURL()) }
        }
    }

    fun addClasspath(vararg urls: URL) {
        urls.forEach {
            if (it.protocol === "file") {
                val path = Paths.get(it.toURI())

                if (path.fileName.toString().endsWith(".war")) {
                    addWar(path)
                } else {
                    this.urls.add(it)
                }

                if (Files.isDirectory(path)) {
                    path.register(classpathWatcher, StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)
                } else {
                    path.parent.register(classpathWatcher, StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)
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

    fun closeClassLoader() {
        onCloseHandlers.forEach { it(this, classLoader) }

        classLoader.close()
    }

    override fun close() {
        closed = true
        classpathWatcher.close()

        FileUtils.deleteRecursive(tempDir)

        if (parentDomain != null) {
            parentDomain.childDomains.remove(this)
        }

        onCloseHandlers.forEach { it(this, classLoader) }

        childDomains.forEach { it.close() }

        classLoader.close()
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
}