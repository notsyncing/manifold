package io.github.notsyncing.manifold.spec

import io.github.notsyncing.manifold.spec.annotations.SceneDef
import io.github.notsyncing.manifold.spec.flow.FlowBuilder
import io.github.notsyncing.manifold.spec.models.*
import io.github.notsyncing.manifold.spec.testcase.TestCaseBuilder

abstract class SpecSceneGroup {
    protected var feature = FeatureInfo()
    protected var permission = PermissionInfo()
    protected var parameters = ParameterListInfo()
    protected var returns = ReturnInfo()
    protected var flow = FlowBuilder()
    protected var cases = TestCaseBuilder()

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

            l.add(SceneSpec(feature, permission, parameters.parameters, returns, flow.build(), cases.build()))

            reset()
        }

        return l
    }
}