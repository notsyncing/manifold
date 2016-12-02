package io.github.notsyncing.manifold.spec.models

import io.github.notsyncing.manifold.spec.flow.FlowInfo
import io.github.notsyncing.manifold.spec.testcase.TestCaseInfo

class SceneSpec(val name: String,
                val feature: FeatureInfo,
                val permission: PermissionInfo,
                val parameters: List<ParameterInfo>,
                val returns: ReturnInfo,
                val flow: FlowInfo,
                val cases: List<TestCaseInfo>) {

}