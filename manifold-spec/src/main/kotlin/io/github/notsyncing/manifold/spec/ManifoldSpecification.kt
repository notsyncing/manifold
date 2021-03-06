package io.github.notsyncing.manifold.spec

import io.github.notsyncing.lightfur.DatabaseManager
import io.github.notsyncing.lightfur.common.LightfurConfig
import io.github.notsyncing.lightfur.common.LightfurConfigBuilder
import io.github.notsyncing.lightfur.integration.jdbc.JdbcPostgreSQLDriver
import io.github.notsyncing.manifold.spec.checkers.SpecChecker
import io.github.notsyncing.manifold.spec.models.ModuleInfo
import io.github.notsyncing.manifold.spec.models.SceneSpec
import io.github.notsyncing.manifold.spec.testcase.TestCaseInfo

abstract class ManifoldSpecification(val useDatabase: Boolean = false) {
    companion object {
        var modules: List<ModuleInfo> = emptyList()
    }

    init {
        if (useDatabase) {
            initDatabase()
        }
    }

    private fun initDatabase() {
        DatabaseManager.setDriver(JdbcPostgreSQLDriver())
        DatabaseManager.getInstance().init(databaseConfig())
    }

    open fun databaseConfig(): LightfurConfig {
        return LightfurConfigBuilder()
                .databaseVersioning(true)
                .build()
    }

    abstract fun spec(): SpecBuilder

    fun specification(spec: SpecBuilder.() -> Unit): SpecBuilder {
        val b = SpecBuilder()
        b.spec()

        return b
    }

    private fun getSpecChecker(): SpecChecker {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        return SpecChecker(this, modules)
    }

    fun checkMetadata() {
        getSpecChecker().runMetadata()
    }

    fun checkMetadata(sceneName: String) {
        getSpecChecker().runMetadata(sceneName)
    }

    fun checkCases() {
        getSpecChecker().runCases()
    }

    fun checkCase(sceneName: String, sceneCase: String) {
        getSpecChecker().runCase(sceneName, sceneCase)
    }

    fun checkScene(sceneName: String) {
        getSpecChecker().runScene(sceneName)
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

    fun getSceneCasesList(): List<Pair<TestCaseInfo?, SceneSpec>> {
        if (modules.isEmpty()) {
            modules = spec().build()
        }

        val l = mutableListOf<Pair<TestCaseInfo?, SceneSpec>>()

        for (m in modules) {
            for (sg in m.sceneGroups) {
                for (s in sg.build()) {
                    if (s.cases.isEmpty()) {
                        l.add(Pair(null, s))
                    } else {
                        for (c in s.cases) {
                            l.add(Pair(c, s))
                        }
                    }
                }
            }
        }

        return l
    }
}