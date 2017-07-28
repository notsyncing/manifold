package io.github.notsyncing.manifold.domain

import java.net.URL
import java.net.URLClassLoader
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory

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
}