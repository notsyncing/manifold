package io.github.notsyncing.manifold.spec

import io.github.notsyncing.manifold.spec.models.ModuleInfo

class SpecBuilder {
    private val modules = mutableListOf<ModuleInfo>()
    private var currModule: ModuleInfo? = null

    infix fun String.has(moduleBlock: SpecBuilder.() -> Unit): Unit {
        val m = ModuleInfo(this)
        modules.add(m)
        currModule = m

        this@SpecBuilder.moduleBlock()
    }

    operator fun SpecSceneGroup.unaryPlus() {
        currModule?.sceneGroups?.add(this)
    }

    fun build(): List<ModuleInfo> {
        return modules
    }
}