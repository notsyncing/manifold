package io.github.notsyncing.manifold.docgen.models

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.action.metadata.SceneDescription
import kotlin.reflect.full.primaryConstructor

open class SceneInfo(var name: String,
                     var fullName: String,
                     var description: String,
                     var parameters: List<ParameterInfo>) {
    constructor(clazz: Class<ManifoldScene<*>>) : this("", "", "", emptyList()) {
        fill(clazz)
    }

    protected fun fill(clazz: Class<ManifoldScene<*>>) {
        val metadata = clazz.getAnnotation(SceneMetadata::class.java)
        name = metadata?.value ?: "未命名场景"
        fullName = clazz.name
        description = clazz.getAnnotation(SceneDescription::class.java)?.value ?: "没有描述"

        parameters = clazz.kotlin.primaryConstructor?.parameters?.map {
            ParameterInfo.from(it)
        } ?: emptyList()
    }
}