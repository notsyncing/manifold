package io.github.notsyncing.manifold.story.drama

import com.alibaba.fastjson.JSON
import io.github.notsyncing.manifold.Manifold

object DramaUtils {
    @JvmStatic
    fun jsonToObject(className: String, json: String, domainName: String? = null): Any? {
        val clazz = if (domainName == null) {
            Class.forName(className)
        } else {
            Manifold.rootDomain.findDomain(domainName)?.get()?.loadClass(className)
        } ?: throw ClassNotFoundException("$className not found in domain $domainName when parsing from JSON $json")

        return JSON.parseObject(json, clazz)
    }

    @JvmStatic
    fun type(className: String, domainName: String? = null): Any? {
        return if (domainName == null) {
            Class.forName(className)
        } else {
            Manifold.rootDomain.findDomain(domainName)?.get()?.loadClass(className)
        } ?: throw ClassNotFoundException("$className not found in domain $domainName")
    }
}