package io.github.notsyncing.manifold.domain

import sun.misc.PerfCounter
import sun.misc.Resource
import sun.misc.URLClassPath
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import java.security.*
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.security.CodeSigner
import java.security.CodeSource
import java.security.ProtectionDomain
import java.security.AllPermission

class DomainClassLoader : URLClassLoader {
    val domainName: String

    constructor(domainName: String, urls: Array<URL>, p1: ClassLoader?) : super(urls, p1) {
        this.domainName = domainName
    }

    constructor(domainName: String, urls: Array<URL>) : super(urls) {
        this.domainName = domainName
    }

    constructor(domainName: String, urls: Array<URL>, p1: ClassLoader?, p2: URLStreamHandlerFactory?) : super(urls, p1, p2) {
        this.domainName = domainName
    }

    constructor(domainName: String, urls: Array<URL>, p1: ClassLoader?, p2: ((String) -> URLStreamHandler)?) : super(urls, p1, p2) {
        this.domainName = domainName
    }

    fun addUrl(url: URL) {
        addURL(url)
    }

    private fun isSealed(var1: String, var2: Manifest): Boolean {
        val var3 = var1.replace('.', '/') + "/"
        var var4: Attributes? = var2.getAttributes(var3)
        var var5: String? = null
        if (var4 != null) {
            var5 = var4.getValue(Attributes.Name.SEALED)
        }

        var4 = var2.mainAttributes

        if (var5 == null && var4 != null) {
            var5 = var4.getValue(Attributes.Name.SEALED)
        }

        if (var5 == null) {
            return false
        }

        return "true".equals(var5!!, ignoreCase = true)
    }

    private fun getAndVerifyPackage(var1: String, var2: Manifest?, var3: URL): Package? {
        val var4 = this.getPackage(var1)
        if (var4 != null) {
            if (var4.isSealed) {
                if (!var4.isSealed(var3)) {
                    throw SecurityException("sealing violation: package $var1 is sealed")
                }
            } else if (var2 != null && this.isSealed(var1, var2)) {
                throw SecurityException("sealing violation: can't seal package $var1: already loaded")
            }
        }

        return var4
    }

    private fun definePackageInternal(var1: String, var2: Manifest?, var3: URL) {
        if (this.getAndVerifyPackage(var1, var2, var3) == null) {
            try {
                if (var2 != null) {
                    this.definePackage(var1, var2, var3)
                } else {
                    this.definePackage(var1, null as String?, null as String?, null as String?, null as String?, null as String?, null as String?, null as URL?)
                }
            } catch (var5: IllegalArgumentException) {
                if (this.getAndVerifyPackage(var1, var2, var3) == null) {
                    throw AssertionError("Cannot find package " + var1)
                }
            }

        }

    }

    private fun defineClass(var1: String, var2: Resource): Class<*> {
        val var3 = System.nanoTime()
        val var5 = var1.lastIndexOf(46.toChar())
        val var6 = var2.codeSourceURL
        if (var5 != -1) {
            val var7 = var1.substring(0, var5)
            val var8 = var2.manifest
            this.definePackageInternal(var7, var8, var6)
        }

        // The dark black magic goes here...
        val perms = Permissions()
        perms.add(AllPermission())
        val domain = ProtectionDomain(CodeSource(null, arrayOfNulls<CodeSigner>(0)), perms)

        val var11 = var2.byteBuffer
        if (var11 != null) {
            val var13 = var2.codeSigners
            PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(var3)
            return this.defineClass(var1, var11, domain)
        } else {
            val var12 = var2.bytes
            val var9 = var2.codeSigners
            PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(var3)
            return this.defineClass(var1, var12, 0, var12.size, domain)
        }
    }

    override fun findClass(var1: String): Class<*> {
        val var2: Class<*>?
        try {
            var2 = AccessController.doPrivileged(PrivilegedExceptionAction<Class<*>> {
                val var1x = var1.replace('.', '/') + ".class"
                val var3 = (URLClassLoader::class.java.getDeclaredField("ucp")
                        .apply { this.isAccessible = true }
                        .get(this@DomainClassLoader) as URLClassPath?)?.getResource(var1x, false)
                if (var3 != null) {
                    try {
                        this@DomainClassLoader.defineClass(var1, var3)
                    } catch (var4: IOException) {
                        throw ClassNotFoundException(var1, var4)
                    }

                } else {
                    null
                }
            }, URLClassLoader::class.java.getDeclaredField("acc")
                    .apply { this.isAccessible = true }
                    .get(this) as AccessControlContext?)
        } catch (var4: PrivilegedActionException) {
            throw var4.exception as ClassNotFoundException
        }

        return var2 ?: throw ClassNotFoundException(var1)
    }
}