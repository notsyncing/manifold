package io.github.notsyncing.manifold.utils

import com.alibaba.fastjson.parser.ParserConfig
import com.alibaba.fastjson.serializer.SerializeConfig
import com.alibaba.fastjson.util.IdentityHashMap
import java.lang.reflect.Type

/**
 *
 * Some DARK BLACK MAGICs are living here
 *
 * You have been warned.
 *
 */
object BlackMagicUtils {
    fun removeFromFastJsonIdentityHashMapIf(map: IdentityHashMap<*, *>, predicate: (Any, Any) -> Boolean) {
        val bucketsField = map.javaClass.getDeclaredField("buckets")
        bucketsField.isAccessible = true
        val buckets = bucketsField.get(map) as Array<Any?>

        val firstNonNullBucket = buckets.firstOrNull { it != null }

        if (firstNonNullBucket == null) {
            return
        }

        val keyField = firstNonNullBucket.javaClass.getDeclaredField("key")
        keyField.isAccessible = true

        val valueField = firstNonNullBucket.javaClass.getDeclaredField("value")
        valueField.isAccessible = true

        val nextField = firstNonNullBucket.javaClass.getDeclaredField("next")
        nextField.isAccessible = true

        for (i in 0..(buckets.size - 1)) {
            val bucket = buckets[i]

            if (bucket == null) {
                continue
            }

            var currBucket: Any? = bucket
            var prevBucket: Any? = null

            while (currBucket != null) {
                val key = keyField.get(currBucket)
                val value = valueField.get(currBucket)
                val next = nextField.get(currBucket)

                if (predicate(key, value)) {
                    if (prevBucket == null) {
                        buckets[i] = next
                    } else {
                        nextField.set(prevBucket, next)
                    }

                    println("Removed $key value $value")
                }

                prevBucket = currBucket
                currBucket = next
            }
        }
    }

    fun clearFastJsonCache(cl: ClassLoader) {
        removeFromFastJsonIdentityHashMapIf(ParserConfig.getGlobalInstance().deserializers) { type, _ ->
            if (type is Class<*>) {
                type.classLoader == cl
            } else {
                false
            }
        }

        val serializersField = SerializeConfig::class.java.getDeclaredField("serializers")
        serializersField.isAccessible = true
        val serializers = serializersField.get(SerializeConfig.getGlobalInstance()) as IdentityHashMap<*, *>

        removeFromFastJsonIdentityHashMapIf(serializers) { type, _ ->
            if (type is Class<*>) {
                type.classLoader == cl
            } else {
                false
            }
        }
    }
}