package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.eventbus.event.ManifoldEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import java.util.*

class TestSceneWithSuccessCond : ManifoldScene<String> {
    companion object {
        const val SCENE_START = 1
        const val SCENE_END = 3
        const val TRANS_START = 4
        const val TRANS_COMMIT = 5
        const val TRANS_END = 6
        const val TRANS_ROLLBACK = 7

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

    private var pass: Boolean = true

    constructor() : super(enableEventNode = false)

    constructor(event: ManifoldEvent) : super(event)

    constructor(pass: Boolean) : this() {
        this.pass = pass
    }

    override fun stage() = async {
        successOn { s -> s == if (pass) "Hello!" else "" }

        useTransaction()

        TestSceneWithSuccessCond.add(TestSceneWithSuccessCond.SCENE_START)

        m(TestAction()).await()

        TestSceneWithSuccessCond.add(TestSceneWithSuccessCond.SCENE_END)

        return@async "Hello!"
    }
}