package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import java.util.*

class TestSceneTransaction(val useTrans: Boolean = true) : ManifoldScene<String>(enableEventNode = false) {
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

    override fun stage() = async {
        if (useTrans) {
            useTransaction()
        }

        add(SCENE_START)

        m(TestAction()).await()

        add(SCENE_END)

        return@async "Hello!"
    }
}