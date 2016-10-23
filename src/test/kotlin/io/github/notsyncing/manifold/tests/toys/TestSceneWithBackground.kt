package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import kotlinx.coroutines.async
import java.util.*

class TestSceneWithBackground() : ManifoldScene<String>(enableEventNode = false) {
    companion object {
        const val SCENE_START = 1
        const val RUN_BG_END = 2
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

    private var keepTrans: Boolean = false

    constructor(keepTrans: Boolean) : this() {
        this.keepTrans = keepTrans
    }

    override fun stage() = async<String> {
        useTransaction()

        add(SCENE_START)

        await(m(TestDbActionSimple()))

        runInBackground(keepTrans) {
            Thread.sleep(500)
            add(RUN_BG_END)
        }

        add(SCENE_END)

        return@async "Hello!"
    }
}