package io.github.notsyncing.manifold.spec.checkers

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.notsyncing.manifold.spec.SpecSceneGroup
import io.github.notsyncing.manifold.spec.models.ModuleInfo
import io.github.notsyncing.manifold.spec.models.SceneSpec

class SpecChecker(private val modules: List<ModuleInfo>) {
    companion object {
        val checkers = mutableListOf<Class<Checker>>()
    }

    private fun scanCheckers() {
        FastClasspathScanner("-scala -kotlin")
                .matchSubclassesOf(Checker::class.java) { checkers.add(it as Class<Checker>) }
                .scan()
    }

    private fun checkScene(scene: SceneSpec) {
        for (checkerClass in checkers) {
            val checker = checkerClass.constructors[0].newInstance(scene)
        }
    }

    private fun checkSceneGroup(sceneGroup: SpecSceneGroup) {
        for (scene in sceneGroup.build()) {
            checkScene(scene)
        }
    }

    private fun checkModule(module: ModuleInfo) {
        for (sceneGroup in module.sceneGroups) {
            checkSceneGroup(sceneGroup)
        }
    }

    fun run() {
        if (checkers.isEmpty()) {
            scanCheckers()
        }

        for (module in modules) {
            checkModule(module)
        }
    }
}