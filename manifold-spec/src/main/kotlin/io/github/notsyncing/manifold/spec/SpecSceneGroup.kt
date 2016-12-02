package io.github.notsyncing.manifold.spec

import io.github.notsyncing.manifold.spec.annotations.SceneDef
import io.github.notsyncing.manifold.spec.flow.FlowBuilder
import io.github.notsyncing.manifold.spec.models.*
import io.github.notsyncing.manifold.spec.testcase.TestCaseBuilder

abstract class SpecSceneGroup {
    val name = this.javaClass.simpleName
    var feature = FeatureInfo()
    var permission = PermissionInfo()
    var parameters = ParameterListInfo()
    var returns = ReturnInfo()
    var flow = FlowBuilder()
    var cases = TestCaseBuilder()

    private fun reset() {
        feature = FeatureInfo()
        permission = PermissionInfo()
        parameters = ParameterListInfo()
        returns = ReturnInfo()
        flow = FlowBuilder()
        cases = TestCaseBuilder()
    }

    fun build(): List<SceneSpec> {
        val sceneDefs = this.javaClass.methods.filter { it.isAnnotationPresent(SceneDef::class.java) }
        val l = mutableListOf<SceneSpec>()

        for (m in sceneDefs) {
            m(this)

            l.add(SceneSpec(m.name, feature, permission, parameters.parameters, returns, flow.build(),
                    cases.build()))

            reset()
        }

        return l
    }
}