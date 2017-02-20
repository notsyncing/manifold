package io.github.notsyncing.manifold.spec

import io.github.notsyncing.manifold.spec.annotations.SceneDef
import io.github.notsyncing.manifold.spec.database.UseDatabase
import io.github.notsyncing.manifold.spec.flow.FlowBuilder
import io.github.notsyncing.manifold.spec.models.*
import io.github.notsyncing.manifold.spec.testcase.TestCaseBuilder

abstract class SpecSceneGroup {
    val name = this::class.java.simpleName
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
        val sceneDefs = this::class.java.methods.filter { it.isAnnotationPresent(SceneDef::class.java) }
        val l = mutableListOf<SceneSpec>()

        for (m in sceneDefs) {
            val useDatabase = m.isAnnotationPresent(UseDatabase::class.java)

            m(this)

            l.add(SceneSpec(m.name, feature, permission, parameters.parameters, returns, flow.build(),
                    cases.build(), useDatabase))

            reset()
        }

        return l
    }
}