package io.github.notsyncing.manifold.bpmn.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.SceneContext
import io.github.notsyncing.manifold.bpmn.InMemorySuspendableSceneStorage
import io.github.notsyncing.manifold.bpmn.SuspendableScene
import io.github.notsyncing.manifold.bpmn.SuspendableSceneScheduler
import io.github.notsyncing.manifold.bpmn.WaitStrategy
import io.github.notsyncing.manifold.bpmn.tests.toys.TestAction1
import io.github.notsyncing.manifold.bpmn.tests.toys.TestSimpleSuspendScene
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SuspendableSceneTest {
    private val storage = SuspendableSceneScheduler.storageProvider as InMemorySuspendableSceneStorage

    @Before
    fun setUp() {
        Manifold.enableFeatureManagement = false
        Manifold.init()
    }

    @After
    fun tearDown() {
        Manifold.destroy().get()
    }

    @Test
    fun testSimpleSuspendableScene() {
        val r1 = Manifold.run(TestSimpleSuspendScene(), "TEST_SESSION").get()
        assertEquals("", r1)

        val stateList = storage.getAsList()
        assertEquals(1, stateList.size)

        val state = stateList[0]
        assertEquals("TEST_SESSION", state.sceneSessionId)
        assertNotNull(state.sceneTaskId)
        assertEquals(WaitStrategy.And, state.awaitStrategy)
        assertEquals(1, state.sceneState.getInteger("label"))
        assertEquals(TestSimpleSuspendScene::class.java.name, state.sceneClassFullName)
        assertEquals(1, state.awaitingActions.size)

        val awaitingAction = state.awaitingActions.entries.first()
        assertEquals(TestAction1::class.java.name, awaitingAction.key)
        assertFalse(awaitingAction.value.executed)
        assertNull(awaitingAction.value.result)

        val action = TestAction1()
        action.context = SceneContext()
        action.context.additionalData.put(SuspendableScene.TASK_ID_FIELD, state.sceneTaskId)

        Manifold.run(action).get()

        Thread.sleep(100)

        assertEquals(TestAction1::class.java.name, awaitingAction.key)
        assertTrue(awaitingAction.value.executed)
        assertEquals("TestAction1", awaitingAction.value.result)

        assertEquals("TestAction1", TestSimpleSuspendScene.finalResult)

        assertEquals(0, storage.getCurrentCount())
    }

    @Test
    fun testSimpleSuspendableSceneWithWrongTaskId() {
        val storage = SuspendableSceneScheduler.storageProvider as InMemorySuspendableSceneStorage

        val r1 = Manifold.run(TestSimpleSuspendScene(), "TEST_SESSION").get()
        assertEquals("", r1)

        val stateList = storage.getAsList()
        val state = stateList[0]

        val action = TestAction1()
        action.context = SceneContext()
        action.context.additionalData.put(SuspendableScene.TASK_ID_FIELD, "ANOTHER_TASK_ID")

        Manifold.run(action).get()

        Thread.sleep(100)

        val awaitingAction = state.awaitingActions.entries.first()
        assertEquals(TestAction1::class.java.name, awaitingAction.key)
        assertFalse(awaitingAction.value.executed)
        assertNull(awaitingAction.value.result)

        assertEquals("", TestSimpleSuspendScene.finalResult)
        assertEquals(1, storage.getCurrentCount())
    }
}