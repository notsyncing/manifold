package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.*

class TestSceneWithBackground() : ManifoldScene<String>(enableEventNode = false) {
    companion object {
        const val SCENE_START = 1
        const val RUN_BG_END = 2
        const val SCENE_END = 3
        const val TRANS_START = 4
        const val TRANS_COMMIT = 5
        const val TRANS_END = 6
        const val TRANS_ROLLBACK = 7
        const val RUN_BG_EXCEPTION = 8

        val list = ArrayList<Int>()
        var ex: Throwable? = null

        fun add(i: Int) {
            synchronized(list) {
                list.add(i)
            }
        }

        fun reset() {
            list.clear()
            ex = null
        }
    }

    private var keepTrans: Boolean = false
    private var throwException: Boolean = false

    constructor(keepTrans: Boolean, throwException: Boolean = false) : this() {
        this.keepTrans = keepTrans
        this.throwException = throwException
    }

    override fun stage() = future {
        useTransaction()

        add(SCENE_START)

        m(TestDbActionSimple()).await()

        runInBackground(keepTrans, {
            Thread.sleep(500)

            if (throwException) {
                throw RuntimeException("test")
            }

            add(RUN_BG_END)
        }, {
            add(RUN_BG_EXCEPTION)
            ex = it
        })

        add(SCENE_END)

        return@future "Hello!"
    }
}