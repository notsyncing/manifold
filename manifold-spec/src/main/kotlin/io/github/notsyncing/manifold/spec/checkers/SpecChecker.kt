package io.github.notsyncing.manifold.spec.checkers

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.notsyncing.manifold.spec.SpecSceneGroup
import io.github.notsyncing.manifold.spec.models.ModuleInfo
import io.github.notsyncing.manifold.spec.models.SceneSpec
import io.github.notsyncing.manifold.spec.testcase.TestCaseInfo
import org.junit.Assert.fail

class SpecChecker(private val modules: List<ModuleInfo>) {
    companion object {
        val checkers = mutableListOf<Class<Checker>>()
        val scanner = FastClasspathScanner("-scala -kotlin")
    }

    private fun scanCheckers() {
        scanner.matchSubclassesOf(Checker::class.java) { checkers.add(it as Class<Checker>) }
                .scan()
    }

    private fun checkCase(checker: Checker, case: TestCaseInfo) {
        println("Check case ${case.behavior} with checker $checker")
        checker.checkCase(case).get()
    }

    private fun checkScene(scene: SceneSpec, type: CheckType, case: TestCaseInfo? = null) {
        for (checkerClass in checkers.toList()) {
            val checker = checkerClass.constructors[0].newInstance(scene) as Checker

            if (case == null) {
                if (type == CheckType.Metadata) {
                    checker.checkMetadata()
                } else if (type == CheckType.Cases) {
                    for (c in scene.cases) {
                        checkCase(checker, c)
                    }
                }
            } else {
                checkCase(checker, case)
            }
        }
    }

    private fun checkSceneGroup(sceneGroup: SpecSceneGroup, type: CheckType) {
        for (scene in sceneGroup.build()) {
            checkScene(scene, type)
        }
    }

    private fun checkModule(module: ModuleInfo, type: CheckType) {
        for (sceneGroup in module.sceneGroups) {
            checkSceneGroup(sceneGroup, type)
        }
    }

    fun runMetadata() {
        if (checkers.isEmpty()) {
            scanCheckers()
        }

        for (m in modules) {
            checkModule(m, CheckType.Metadata)
        }
    }

    fun runMetadata(sceneName: String) {
        if (checkers.isEmpty()) {
            scanCheckers()
        }

        for (m in modules) {
            println("In module: ${m.name}")

            for (sg in m.sceneGroups) {
                println("In scene group: ${sg.name}")

                for (s in sg.build()) {
                    if (s.name == sceneName) {
                        println("In scene: ${s.name}")

                        checkScene(s, CheckType.Metadata)
                        return
                    }
                }
            }
        }

        fail("Scene $sceneName not found")
    }

    fun runCases() {
        if (checkers.isEmpty()) {
            scanCheckers()
        }

        for (module in modules) {
            checkModule(module, CheckType.Cases)
        }
    }

    fun runCase(sceneCase: String) {
        if (checkers.isEmpty()) {
            scanCheckers()
        }

        for (m in modules) {
            println("In module: ${m.name}")

            for (sg in m.sceneGroups) {
                println("In scene group: ${sg.name}")

                for (s in sg.build()) {
                    for (c in s.cases) {
                        if (c.behavior == sceneCase) {
                            println("In scene: ${s.name}")
                            println("In case: ${c.behavior}")

                            checkScene(s, CheckType.Cases, c)
                            return
                        }
                    }
                }
            }
        }

        fail("Scene case $sceneCase not found")
    }

    fun runScene(sceneName: String) {
        if (checkers.isEmpty()) {
            scanCheckers()
        }

        for (m in modules) {
            println("In module: ${m.name}")

            for (sg in m.sceneGroups) {
                println("In scene group: ${sg.name}")

                for (s in sg.build()) {
                    if (s.name == sceneName) {
                        println("In scene: ${s.name}")

                        checkScene(s, CheckType.Metadata)
                        checkScene(s, CheckType.Cases)
                        return
                    }
                }
            }
        }

        fail("Scene $sceneName not found")
    }
}