package io.github.notsyncing.manifold.docgen.models

import io.github.notsyncing.manifold.action.metadata.Parameter
import kotlin.reflect.KParameter

class ParameterInfo(val name: String,
                    val realName: String,
                    val description: String,
                    val type: String) {
    companion object {
        fun from(param: KParameter): ParameterInfo {
            val metadata = param.annotations.firstOrNull { it is Parameter } as Parameter?
            val name = metadata?.value ?: ""
            val realName = param.name ?: "未命名参数"
            val description = metadata?.description ?: "没有描述"
            val type = param.type.toString()

            return ParameterInfo(name, realName, description, type)
        }
    }
}