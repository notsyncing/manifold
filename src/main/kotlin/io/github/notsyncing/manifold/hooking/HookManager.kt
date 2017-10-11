package io.github.notsyncing.manifold.hooking

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.domain.ManifoldDomain
import io.github.notsyncing.manifold.utils.removeIf
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Logger

class HookManager {
    private val hooks = ConcurrentHashMap<String, ConcurrentLinkedQueue<Class<Hook<*>>>>()

    private val log = Logger.getLogger(javaClass.simpleName)

    private fun makeRealName(name: String, domain: String? = null): String {
        if ((domain != null) && (domain != ManifoldDomain.ROOT)) {
            return "${domain}_$name"
        } else {
            return name
        }
    }

    fun registerHook(name: String, domain: String? = null, hook: Class<Hook<*>>) {
        hooks.compute(makeRealName(name, domain)) { _, v ->
            if (v == null) {
                val l = ConcurrentLinkedQueue<Class<Hook<*>>>()
                l.add(hook)
                l
            } else {
                if (!v.contains(hook)) {
                    v.add(hook)
                } else {
                    log.warning("Hook $v already contained in $name, will be skipped")
                }

                v
            }
        }
    }

    fun registerHook(domain: String? = null, hook: Class<Hook<*>>) {
        val name = if (hook.javaClass.isAnnotationPresent(HookingFor::class.java)) {
            hook.getAnnotation(HookingFor::class.java).value
        } else {
            hook.name
        }

        registerHook(name, domain, hook)
    }

    fun getHooks(name: String, domain: String? = null): List<Class<Hook<*>>> {
        val list = hooks[makeRealName(name, domain)]

        return if (list == null) {
            emptyList()
        } else {
            list.toList()
        }
    }

    fun <T> runHooks(name: String, domain: String? = null, inputValue: T?) = future {
        val matchedHooks = getHooks(name, domain)

        if (matchedHooks.isEmpty()) {
            return@future inputValue
        }

        var result = inputValue

        for (h in matchedHooks) {
            val hh = Manifold.dependencyProvider?.get(h) as Hook<T?>?

            if (hh == null) {
                log.warning("Cannot instantiate hook class $h")
                continue
            }

            result = hh.execute(result).await()
        }

        result
    }

    fun <T, H: Hook<T>> runHooks(hookClass: Class<H>, domain: String? = null, inputValue: T?): CompletableFuture<T?> {
        return runHooks(hookClass.name, domain, inputValue)
    }

    fun removeFromCacheIf(predicate: (Class<Hook<*>>) -> Boolean) {
        hooks.forEach { (_, hooks) ->
            hooks.removeIf(predicate)
        }

        hooks.removeIf { (_, hooks) -> hooks.isEmpty() }
    }

    fun reset() {
        hooks.clear()
    }
}