package io.github.notsyncing.manifold.spec.checkers

import com.alibaba.fastjson.JSON
import io.github.notsyncing.lightfur.DatabaseManager
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.manifold.feature.FeatureAuthenticator
import io.github.notsyncing.manifold.spec.ActionInvokeRecorder
import io.github.notsyncing.manifold.spec.ManifoldSpecification
import io.github.notsyncing.manifold.spec.flow.FlowActionItem
import io.github.notsyncing.manifold.spec.flow.FlowItem
import io.github.notsyncing.manifold.spec.models.SceneSpec
import io.github.notsyncing.manifold.spec.testcase.TestCaseInfo
import kotlinx.coroutines.async
import org.junit.Assert.*
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.jvmErasure

class SceneChecker(spec: ManifoldSpecification, scene: SceneSpec) : Checker(spec, scene) {
    companion object {
        val sceneClasses = mutableListOf<Class<ManifoldScene<*>>>()
    }

    private var currSceneClass: Class<ManifoldScene<*>>? = null
    private var currDatabaseName: String = ""

    init {
        if (sceneClasses.isEmpty()) {
            scanner.matchSubclassesOf(ManifoldScene::class.java) { sceneClasses.add(it as Class<ManifoldScene<*>>) }
                    .scan()
        }

        currSceneClass = sceneClasses.firstOrNull {
            val metadata = it.getAnnotation(SceneMetadata::class.java)
            metadata?.value == scene.name
        }
    }

    private fun checkFeature() {
        val feature = currSceneClass!!.getAnnotation(Feature::class.java)

        if (feature == null) {
            fail("Scene ${scene.name} has no @${Feature::class.java.simpleName} annotation!")
            return
        }

        assertEquals("Scene ${scene.name} has wrong feature name: expected ${scene.feature.name}, actual ${feature.value}",
                scene.feature.name, feature.value)
        assertArrayEquals("Scene ${scene.name} has wrong feature groups: expected ${JSON.toJSONString(scene.feature.groups)}, actual ${JSON.toJSONString(feature.groups)}",
                scene.feature.groups, feature.groups)
        assertArrayEquals("Scene ${scene.name} has wrong default special authentications: expected ${JSON.toJSONString(scene.feature.defaultSpecialAuths)}, actual ${JSON.toJSONString(feature.defaultSpecialAuths)}",
                scene.feature.defaultSpecialAuths, feature.defaultSpecialAuths)
        assertEquals("Scene ${scene.name} has wrong internal property: expected ${scene.feature.internal}, actual ${feature.internal}",
                scene.feature.internal, feature.internal)
    }

    private fun checkPermission() {
        val (module, auth) = FeatureAuthenticator.getAuth(scene.feature.name!!)

        assertEquals("Scene ${scene.name} has wrong permission module: expected ${scene.permission.module}, actual $module",
                scene.permission.module, module)
        assertEquals("Scene ${scene.name} has wrong permission type: expected ${scene.permission.type}, actual $auth",
                scene.permission.type, auth)
    }

    private fun getCurrentSceneMatchedConstructor(): KFunction<ManifoldScene<*>>? {
        for (c in currSceneClass!!.kotlin.constructors) {
            if (c.parameters.size != scene.parameters.size) {
                continue
            }

            var found = true

            for (i in 0..scene.parameters.size - 1) {
                if (c.parameters[i].name != scene.parameters[i].fieldName) {
                    found = false
                    break
                }

                if (c.parameters[i].type.jvmErasure.java != scene.parameters[i].type) {
                    found = false
                    break
                }

                if (c.parameters[i].type.isMarkedNullable != scene.parameters[i].nullable) {
                    found = false
                    break
                }
            }

            if (found) {
                return c
            }
        }

        return null
    }

    private fun checkParameters() {
        val c = getCurrentSceneMatchedConstructor()

        if (c == null) {
            fail("No constructor of ${scene.name} matches the parameters expected: ${JSON.toJSONString(scene.parameters)}")
        }
    }

    private fun checkReturns() {
        if (scene.returns.returnType == null) {
            return
        }

        val returnType = currSceneClass!!.kotlin.members
                .firstOrNull { it.name == ManifoldScene<*>::stage.name }?.returnType?.arguments?.get(0)?.type?.jvmErasure?.java
        assertEquals("Scene ${scene.name} has wrong return type: expected ${scene.returns.returnType}, actual: $returnType",
                scene.returns.returnType, returnType)
    }

    override fun checkMetadata() {
        if (currSceneClass == null) {
            fail("No scene named ${scene.name} was found!")
            return
        }

        checkFeature()
        checkPermission()
        checkParameters()
        checkReturns()
    }

    private fun makeSceneFromCase(case: TestCaseInfo): Pair<ManifoldScene<*>?, String?> {
        val c = getCurrentSceneMatchedConstructor()

        if (c == null) {
            fail("No constructor of ${scene.name} matches the parameters expected: ${JSON.toJSONString(scene.parameters)}")
            return Pair(null, "")
        }

        val params = mutableListOf<Any?>()

        for (p in c.parameters) {
            params.add(case.parameters[p.name])
        }

        val s = c.call(*params.toTypedArray())

        println("Parameters: ${JSON.toJSONString(params)}")

        return Pair(s, case.parameters[case.sessionIdentifier] as String?)
    }

    private fun resolveActionRoutes(case: TestCaseInfo): Pair<List<String>, TestCaseInfo.TestCaseExitPoint?> {
        val exitPoint = scene.flow.ends.firstOrNull { it.text == case.exit.exitName }

        if (exitPoint == null) {
            fail("No exit point matches ${case.exit.exitName} in scene ${scene.name}")
            return Pair(emptyList(), null)
        }

        val l = mutableListOf<String>()
        var currFlowItem: FlowItem? = exitPoint.previous

        while (currFlowItem != null) {
            if (currFlowItem is FlowActionItem) {
                l.add(currFlowItem.text)
            }

            currFlowItem = currFlowItem.previous
        }

        return Pair(l.reversed(), case.exit)
    }

    private fun initDatabase() = async<Unit> {
        if (!scene.useDatabase) {
            return@async
        }

        currDatabaseName = "manifold_spec_test_db_" + Math.random().toString().substring(3)

        await(DatabaseManager.getInstance().createDatabase(currDatabaseName, true))
        await(DatabaseManager.getInstance().upgradeDatabase(currDatabaseName))
    }

    private fun destroyDatabase() = async<Unit> {
        if (!scene.useDatabase) {
            return@async
        }

        await(DatabaseManager.getInstance().dropDatabase(currDatabaseName, true))
    }

    override fun checkCase(case: TestCaseInfo) = async<Unit> {
        val (expectedActions, expectedExitPoint) = resolveActionRoutes(case)
        val (s, sessId) = makeSceneFromCase(case)

        if (s == null) {
            fail("Failed to create instance of scene ${scene.name}")
            return@async
        }

        ActionInvokeRecorder.reset()

        try {
            await(initDatabase())

            await(CompletableFuture.runAsync { case.otherInit?.invoke() })

            val actualResult = await(Manifold.run(s, sessId))
            val actualActions = ActionInvokeRecorder.recorded.map { (action, _) -> action }

            if (expectedExitPoint!!.hasResult) {
                assertEquals("Scene ${scene.name} returned unexpected result: expected ${expectedExitPoint.result}, actual $actualResult",
                        expectedExitPoint.result, actualResult)
            }

            if (expectedExitPoint.resultInto != null) {
                expectedExitPoint.resultInto!!.value = actualResult
            }

            println("Result: $actualResult")

            assertArrayEquals("Scene ${scene.name} run through unexpected flow: expected ${JSON.toJSONString(expectedActions)}, actual ${JSON.toJSONString(actualActions)}",
                    expectedActions.toTypedArray(), actualActions.toTypedArray())

            println("Flow:")

            if (actualActions.isNotEmpty()) {
                actualActions.forEach(::println)
            } else {
                println("<Empty>")
            }

            println()

            if (case.additionalConditions.isNotEmpty()) {
                println("Additional checks:")

                for (cond in case.additionalConditions) {
                    println(cond.name)
                    assertTrue("Scene ${scene.name} has unmet condition: ${cond.name} returned false", cond.cond())
                }

                println()
            }
        } catch (e: Throwable) {
            println("Failed in scene ${scene.name}: ${e.message}")
            e.printStackTrace()

            await(destroyDatabase())

            fail(e.message)
        }

        await(destroyDatabase())
    }
}