package io.github.notsyncing.manifold.rules

import jdk.nashorn.api.scripting.ScriptObjectMirror
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import javax.script.ScriptEngineManager

object RuleEngine {
    private val engine = ScriptEngineManager().getEngineByName("nashorn")
    private val rules = ConcurrentHashMap<String, ScriptObjectMirror>()

    private lateinit var provider: RuleStorageProvider

    private val log = Logger.getLogger(javaClass.simpleName)

    fun setProvider(p: RuleStorageProvider) {
        provider = p
    }

    fun init() {
    }

    fun destroy() {
        rules.clear()
    }

    fun run(ruleName: String, context: Any? = null, params: Map<String, Any?> = emptyMap(), vararg additionalParams: Any?) = future {
        try {
            val functor: ScriptObjectMirror

            if (!rules.containsKey(ruleName)) {
                val rule = provider.get(ruleName).await()

                if (rule == null) {
                    throw Exception("No rule with name $ruleName found!")
                }

                functor = (engine.eval(rule.content) as ScriptObjectMirror)
                        .apply {
                            this.freeze()
                            this.seal()
                        }

                rules.put(ruleName, functor)
            } else {
                functor = rules[ruleName]!!
            }

            val ruleEnv = mutableMapOf<String, Any?>()
            ruleEnv.put("name", ruleName)

            functor.call(ruleEnv, context, params, *additionalParams)
        } catch (e: Exception) {
            throw Exception("An exception occured when running rule $ruleName", e)
        }
    }

    fun ruleChanged(ruleName: String) {
        rules.remove(ruleName)
    }
}