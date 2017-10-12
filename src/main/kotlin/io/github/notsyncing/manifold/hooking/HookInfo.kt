package io.github.notsyncing.manifold.hooking

import java.util.*

class HookInfo(var clazz: Class<Hook<*>>,
               val source: String? = null) {
    override fun equals(other: Any?): Boolean {
        if (other !is HookInfo) {
            return super.equals(other)
        }

        return (clazz == other.clazz) && (source == other.source)
    }

    override fun hashCode(): Int {
        return Objects.hash(clazz, source)
    }
}