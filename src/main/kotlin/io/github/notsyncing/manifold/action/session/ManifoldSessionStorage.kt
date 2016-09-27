package io.github.notsyncing.manifold.action.session

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ManifoldSessionStorage : ManifoldSessionStorageProvider {
    private val storage = ConcurrentHashMap<String, ConcurrentHashMap<String, Any>>()
    private val timer = Executors.newScheduledThreadPool(1)

    init {
        timer.scheduleAtFixedRate({
            storage.forEach { m ->
                m.value.filterValues { it is TimedVar<*> }
                        .mapValues { it.value as TimedVar<*> }
                        .forEach {
                            val t = --it.value.time

                            if (t <= 0) {
                                m.value.remove(it.key)
                            }
                        }
            }
        }, 0, 1, TimeUnit.MINUTES)
    }

    override fun <T> get(sessionIdentifier: String, key: String): T? {
        val r = storage[sessionIdentifier]?.get(key)

        if (r == null) {
            return null
        }

        if (r is TimedVar<*>) {
            return r.value as T?
        }

        return r as T?
    }

    override fun <T> put(sessionIdentifier: String, key: String, value: Any): T? {
        if (!storage.containsKey(sessionIdentifier)) {
            storage[sessionIdentifier] = ConcurrentHashMap()
        }

        return storage[sessionIdentifier]!!.put(key, value) as T?
    }

    override fun <T> remove(sessionIdentifier: String, key: String): T? {
        if (!storage.containsKey(sessionIdentifier)) {
            return null
        }

        return storage[sessionIdentifier]!!.remove(key) as T?
    }

    override fun has(sessionIdentifier: String, key: String): Boolean {
        if (!storage.containsKey(sessionIdentifier)) {
            return false
        }

        return storage[sessionIdentifier]!!.containsKey(key)
    }

    override fun clear(sessionIdentifier: String) {
        if (storage.containsKey(sessionIdentifier)) {
            storage[sessionIdentifier]!!.clear()
        }
    }

    override fun clear() {
        storage.clear()
    }
}