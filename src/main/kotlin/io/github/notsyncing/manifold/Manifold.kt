package io.github.notsyncing.manifold

import java.util.concurrent.ConcurrentHashMap

object Manifold {
    var dependencyProvider: ManifoldDependencyProvider? = null
    var actionPool: MutableMap<Class<*>, ManifoldAction<*>> = ConcurrentHashMap()

    fun reset() {
        dependencyProvider = null

        actionPool.clear()
    }

    fun registerAction(action: ManifoldAction<*>) {
        actionPool[action.javaClass] = action
    }

    fun <A: ManifoldAction<T>, T> getAction(action: Class<A>) = actionPool[action]

    inline fun <reified A: ManifoldAction<*>> from(): A {
        val a: A

        if (!actionPool.containsKey(A::class.java)) {
            if (dependencyProvider != null) {
                a = dependencyProvider!!.get(A::class.java)!!
            } else {
                a = A::class.java.newInstance()
            }

            actionPool[A::class.java] = a
        } else {
            a = actionPool[A::class.java]!! as A
        }

        return a
    }
}