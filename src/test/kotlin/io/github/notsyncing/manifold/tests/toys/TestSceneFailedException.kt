package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.SceneFailedException
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.*

class TestSceneFailedException : ManifoldScene<String> {
    companion object {
        const val SCENE_START = 1
        const val SCENE_END = 3
        const val TRANS_START = 4
        const val TRANS_COMMIT = 5
        const val TRANS_END = 6

        val list = ArrayList<Int>()

        fun add(i: Int) {
            synchronized(list) {
                list.add(i)
            }
        }

        fun reset() {
            list.clear()
        }
    }

    constructor() : super(enableEventNode = false)

    constructor(event: ManifoldEvent) : super(event)

    override fun stage() = future<String> {
        useTransaction()

        add(SCENE_START)

        m(TestTransActionSimple()).await()

        throw SceneFailedException("Failed!")
    }
}