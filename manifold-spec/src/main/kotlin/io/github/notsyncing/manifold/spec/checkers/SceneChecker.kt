package io.github.notsyncing.manifold.spec.checkers

import com.alibaba.fastjson.JSON
import io.github.notsyncing.lightfur.DatabaseManager
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.Probe
import io.github.notsyncing.manifold.action.SceneMetadata
import io.github.notsyncing.manifold.feature.Feature
import io.github.notsyncing.manifold.feature.FeatureAuthenticator
import io.github.notsyncing.manifold.spec.ActionInvokeRecorder
import io.github.notsyncing.manifold.spec.ManifoldSpecification
import io.github.notsyncing.manifold.spec.flow.FlowActionItem
import io.github.notsyncing.manifold.spec.flow.FlowCheckCondItem
import io.github.notsyncing.manifold.spec.flow.FlowItem
import io.github.notsyncing.manifold.spec.models.ParameterInfo
import io.github.notsyncing.manifold.spec.models.SceneSpec
import io.github.notsyncing.manifold.spec.testcase.TestCaseInfo
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.junit.Assert.*
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
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

    private fun compareParameter(expected: ParameterInfo, actual: KParameter): Boolean {
        if (expected.nullable) {
            if ((actual.type.jvmErasure.java == java.lang.Long::class.java) && (expected.type == Long::class.java)) {
                return true
            }

            if ((actual.type.jvmErasure.java == java.lang.Integer::class.java) && (expected.type == Int::class.java)) {
                return true
            }

            if ((actual.type.jvmErasure.java == java.lang.Boolean::class.java) && (expected.type == Boolean::class.java)) {
                return true
            }

            if ((actual.type.jvmErasure.java == java.lang.Double::class.java) && (expected.type == Double::class.java)) {
                return true
            }

            if ((actual.type.jvmErasure.java == java.lang.Float::class.java) && (expected.type == Float::class.java)) {
                return true
            }

            if ((actual.type.jvmErasure.java == java.lang.Character::class.java) && (expected.type == Char::class.java)) {
                return true
            }

            if ((actual.type.jvmErasure.java == java.lang.Byte::class.java) && (expected.type == Byte::class.java)) {
                return true
            }
        }

        return actual.type.jvmErasure.java == expected.type
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

                if (!compareParameter(scene.parameters[i], c.parameters[i])) {
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

    private fun resolveActionRoutes(case: TestCaseInfo, currentScene: ManifoldScene<*>): Pair<List<String>, TestCaseInfo.TestCaseExitPoint?> {
        val exitPoint = scene.flow.ends.firstOrNull { it.text == case.exit.exitName }
        val probes = mutableMapOf<String, Any?>()

        currentScene::class.java.kotlin.memberProperties.filter { it.annotations.any { it.annotationClass == Probe::class } }
                .forEach {
                    it.isAccessible = true

                    var name = it.findAnnotation<Probe>()?.value

                    if ((name == null) || (name == "")) {
                        name = it.name
                    }

                    probes.put(name, it.getter.call(currentScene))
                }

        currentScene::class.java.kotlin.declaredMemberProperties.filter { it.annotations.all { it.annotationClass != Probe::class } }
                .forEach {
                    it.isAccessible = true
                    probes.put(it.name, it.getter.call(currentScene))
                }

        if (exitPoint == null) {
            fail("No exit point matches ${case.exit.exitName} in scene ${scene.name}")
            return Pair(emptyList(), null)
        }

        val l = mutableListOf<String>()
        var currFlowItem: FlowItem? = exitPoint.previous

        while (currFlowItem != null) {
            if (currFlowItem.previous is FlowCheckCondItem) {
                val checkItem = currFlowItem.previous as FlowCheckCondItem
                val pass = checkItem.check(probes)
                currFlowItem = if (pass) checkItem.nextTrue else checkItem.nextFalse

                if (currFlowItem == null) {
                    fail("Check item ${checkItem.text} is $pass, but it has no matching branch! Scene: ${scene.name}")
                    return Pair(emptyList(), null)
                }
            }

            if (currFlowItem is FlowActionItem) {
                l.add(currFlowItem.text)
            }

            currFlowItem = currFlowItem.previous
        }

        return Pair(l.reversed(), case.exit)
    }

    private fun initDatabase() = future {
        if (!scene.useDatabase) {
            return@future
        }

        currDatabaseName = "manifold_spec_test_db_" + Math.random().toString().substring(3)

        DatabaseManager.getInstance().createDatabase(currDatabaseName, true).await()
        DatabaseManager.getInstance().upgradeDatabase(currDatabaseName).await()
    }

    private fun destroyDatabase() = future {
        if (!scene.useDatabase) {
            return@future
        }

        DatabaseManager.getInstance().dropDatabase(currDatabaseName, true).await()
    }

    override fun checkCase(case: TestCaseInfo) = future {
        val (s, sessId) = makeSceneFromCase(case)

        if (s == null) {
            fail("Failed to create instance of scene ${scene.name}")
            return@future
        }

        ActionInvokeRecorder.reset()

        try {
            initDatabase().await()

            CompletableFuture.runAsync { case.otherInit?.invoke() }.await()

            val actualResult = Manifold.run(s, sessId).await()
            val actualActions = ActionInvokeRecorder.recorded.map { it.name }

            val (expectedActions, expectedExitPoint) = resolveActionRoutes(case, s)

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
                actualActions.forEach { println(it) }
            } else {
                println("<Empty>")
            }

            println()

            if (case.additionalConditions.isNotEmpty()) {
                println("Additional checks:")

                for (cond in case.additionalConditions) {
                    println(cond.name)

                    val result = CompletableFuture.supplyAsync { cond.cond() }.await()
                    assertTrue("Scene ${scene.name} has unmet condition: ${cond.name} returned false", result)
                }

                println()
            }
        } catch (e: Throwable) {
            println("Failed in scene ${scene.name}: ${e.message}")
            e.printStackTrace()

            destroyDatabase().await()

            fail(e.message)
        }

        destroyDatabase().await()
    }
}