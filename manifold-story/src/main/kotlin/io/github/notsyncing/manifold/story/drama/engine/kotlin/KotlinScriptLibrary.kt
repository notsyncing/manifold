package io.github.notsyncing.manifold.story.drama.engine.kotlin

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.hooking.Hook
import io.github.notsyncing.manifold.story.drama.DramaActionContext
import io.github.notsyncing.manifold.story.drama.DramaManager
import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

typealias KotlinScriptAction<R> = (DramaActionContext, JSONObject, JSONObject?) -> CompletableFuture<R>

class Role(private val permissionName: String? = null,
           private val permissionType: String? = null) {
    fun <R> on(actionName: String, handler: KotlinScriptAction<R>, isInternal: Boolean) {
        val h = { context: DramaActionContext, params: String, permParams: String? ->
            handler(context, JSON.parseObject(params),
                    if (permParams == null) null else JSON.parseObject(permParams))
        }

        val callable = KotlinScriptDramaEngine.toCallable(h)

        DramaManager.registerAction(actionName, isInternal, permissionName, permissionType, callable,
                DramaManager.currentFile ?: "", null)
    }

    fun <R> on(actionName: String, handler: KotlinScriptAction<R>) {
        on(actionName, handler, false)
    }
}

object TypeUtils {
    private fun anyToType(any: Any?, type: Type): Any? {
        if (type == Int::class.java) {
            if (any is Int) {
                return any
            } else {
                return any.toString().toInt()
            }
        } else if (type == Long::class.java) {
            if (any is Long) {
                return any
            } else {
                return any.toString().toLong()
            }
        } else if (type == String::class.java) {
            if (any is String) {
                return any
            } else {
                return any.toString()
            }
        } else if (type == Boolean::class.java) {
            if (any is Boolean) {
                return any
            } else {
                return any.toString().toBoolean()
            }
        } else if (type == Float::class.java) {
            if (any is Float) {
                return any
            } else {
                return any.toString().toFloat()
            }
        } else if (type == Double::class.java) {
            if (any is Double) {
                return any
            } else {
                return any.toString().toDouble()
            }
        } else if (type == Byte::class.java) {
            if (any is Byte) {
                return any
            } else {
                return any.toString().toByte()
            }
        } else if (type == Char::class.java) {
            if (any is Char) {
                return any
            } else {
                return any.toString().toInt()
            }
        } else if (type == Short::class.java) {
            if (any is Short) {
                return any
            } else {
                return any.toString().toShort()
            }
        } else if (type == JSONObject::class.java) {
            if (any is JSONObject) {
                return any
            } else {
                return JSON.parse(any.toString())
            }
        } else if (type == JSONArray::class.java) {
            if (any is JSONArray) {
                return any
            } else {
                return JSON.parseArray(any.toString())
            }
        } else {
            val s: String
            val from = any.toString()

            if ((from.startsWith("[")) && (from.endsWith("]"))) {
                s = from
            } else if ((from.startsWith("{")) && (from.endsWith("}"))) {
                s = from
            } else {
                s = "\"$from\""
            }

            return JSON.parseObject(s, type)
        }
    }

    private fun Any?.toType(type: Type) = anyToType(this, type)

    fun processParameters(function: KFunction<*>,
                                  parameters: JSONObject): Map<KParameter, Any?> {
        val targetParams = mutableMapOf<KParameter, Any?>()
        val params = function.parameters
        val o = mutableMapOf<String, Any?>()

        parameters.forEach { (k, v) -> o[k] = v }

        if (!params.isEmpty()) {
            for (p in params) {
                if (p.kind == KParameter.Kind.INSTANCE) {
                    continue
                } else if (p.kind != KParameter.Kind.VALUE) {
                    continue
                }

                val v: Any?
                val type = p.type.jvmErasure.java

                if (o.containsKey(p.name)) {
                    v = o[p.name].toType(type)
                } else {
                    if (p.isOptional) {
                        continue
                    }

                    v = null
                }

                targetParams[p] = v
            }
        }

        return targetParams
    }
}

fun <F: Function<R>, R> expand(obj: JSONObject, body: F): R {
    val function = body.reflect()!!
    val realFunction = body.javaClass.methods.first { it.name == "invoke" }.apply { this.isAccessible = true }

    val params = TypeUtils.processParameters(function, obj)

    val actualParams = Array<Any?>(params.size) { null }

    for (p in params) {
        actualParams[p.key.index] = p.value
    }

    return realFunction.invoke(body, *actualParams) as R
}

fun hooking(name: String, hook: Hook<*>) {
    val hookSource = DramaManager.currentFile + "?" + name

    Manifold.hooks.registerHook(name, null, hook.javaClass, hookSource)
}