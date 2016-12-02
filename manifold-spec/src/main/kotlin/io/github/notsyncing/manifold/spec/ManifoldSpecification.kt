package io.github.notsyncing.manifold.spec

import io.github.notsyncing.manifold.spec.checkers.SpecChecker
import io.github.notsyncing.manifold.spec.models.ModuleInfo

abstract class ManifoldSpecification {
    companion object {
        var modules: List<ModuleInfo> = emptyList()
    }

    abstract fun spec(): SpecBuilder

    fun specification(spec: SpecBuilder.() -> Unit): SpecBuilder {
        val b = SpecBuilder()
        b.spec()

        return b
    }

    fun checkMetadata() {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        SpecChecker(modules).runMetadata()
    }

    fun checkMetadata(sceneName: String) {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        SpecChecker(modules).runMetadata(sceneName)
    }

    fun checkCases() {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        SpecChecker(modules).runCases()
    }

    fun checkCase(sceneName: String, sceneCase: String) {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        SpecChecker(modules).runCase(sceneName, sceneCase)
    }

    fun checkScene(sceneName: String) {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        SpecChecker(modules).runScene(sceneName)
    }

    fun getSceneNameList(): List<String> {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        val l = mutableListOf<String>()

        for (m in modules) {
            for (sg in m.sceneGroups) {
                for (s in sg.build()) {
                    l.add(s.name)
                }
            }
        }

        return l
    }

    fun getSceneCasesList(): List<Pair<String, String>> {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        val l = mutableListOf<Pair<String, String>>()

        for (m in modules) {
            for (sg in m.sceneGroups) {
                for (s in sg.build()) {
                    if (s.cases.isEmpty()) {
                        l.add(Pair("", s.name))
                    } else {
                        for (c in s.cases) {
                            l.add(Pair(c.behavior, s.name))
                        }
                    }
                }
            }
        }

        return l
    }
}