package io.github.notsyncing.manifold.bpmn.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.SceneContext
import io.github.notsyncing.manifold.bpmn.BpmnStorageProvider
import io.github.notsyncing.manifold.bpmn.InMemorySuspendableSceneStorage
import io.github.notsyncing.manifold.bpmn.SuspendableScene
import io.github.notsyncing.manifold.bpmn.SuspendableSceneScheduler
import io.github.notsyncing.manifold.bpmn.tests.toys.SimpleBpmnScene
import io.github.notsyncing.manifold.bpmn.tests.toys.TestAction2
import io.github.notsyncing.manifold.bpmn.tests.toys.TestAction3
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

class BpmnSceneTest {
    private val simpleDiagram = readStringFromResource("/simple.bpmn")
    private val exclusiveBranchDiagram = readStringFromResource("/exclusive_branch.bpmn")
    private val storage = SuspendableSceneScheduler.storageProvider as InMemorySuspendableSceneStorage

    private val bpmnStorage = object : BpmnStorageProvider {
        override fun getDiagram(name: String): CompletableFuture<String> {
            return CompletableFuture.completedFuture(when (name) {
                "simple" -> simpleDiagram
                "exclusive_branch" -> exclusiveBranchDiagram
                else -> throw UnsupportedOperationException("Unknown diagram name $name")
            })
        }

        override fun saveDiagram(name: String, data: String): CompletableFuture<Unit> {
            TODO("not implemented")
        }
    }

    private fun readStringFromResource(path: String): String {
        return String(Files.readAllBytes(Paths.get(javaClass.getResource(path).toURI())))
    }

    private fun waitUntilNotNull(expr: () -> Any?, timeOut: Int) {
        var count = timeOut * 10

        while (expr() == null) {
            Thread.sleep(100)

            count--

            if (count <= 0) {
                break
            }
        }
    }

    @Before
    fun setUp() {
        Manifold.enableFeatureManagement = false
        Manifold.init()

        Manifold.dependencyProvider!!.registerAs(bpmnStorage, BpmnStorageProvider::class.java)

        SimpleBpmnScene.reset()
    }

    @After
    fun tearDown() {
        Manifold.destroy().get()
    }

    @Test
    fun testSimpleBpmn() {
        val r = Manifold.run(SimpleBpmnScene("simple")).get()
        assertNull(r)

        val stateList = storage.getAsList()
        val state = stateList[0]

        val action = TestAction2()
        action.context = SceneContext()
        action.context.additionalData.put(SuspendableScene.TASK_ID_FIELD, state.sceneTaskId)

        Manifold.run(action).get()

        waitUntilNotNull({ SimpleBpmnScene.result }, 20)

        assertEquals("TestAction2", SimpleBpmnScene.result)
    }

    @Test
    fun testExclusiveBranchBpmnA() {
        Manifold.run(SimpleBpmnScene("exclusive_branch")).get()

        val stateList = storage.getAsList()
        val state = stateList[0]

        TestAction3.result = true
        val action = TestAction3()
        action.context = SceneContext()
        action.context.additionalData.put(SuspendableScene.TASK_ID_FIELD, state.sceneTaskId)

        Manifold.run(action).get()

        waitUntilNotNull({ SimpleBpmnScene.result }, 20)

        assertEquals("TestAction2", SimpleBpmnScene.result)
    }

    @Test
    fun testExclusiveBranchBpmnB() {
        Manifold.run(SimpleBpmnScene("exclusive_branch")).get()

        val stateList = storage.getAsList()
        val state = stateList[0]

        TestAction3.result = false
        val action = TestAction3()
        action.context = SceneContext()
        action.context.additionalData.put(SuspendableScene.TASK_ID_FIELD, state.sceneTaskId)

        Manifold.run(action).get()

        waitUntilNotNull({ SimpleBpmnScene.result }, 20)

        assertEquals("<NULL>", SimpleBpmnScene.result)
    }
}