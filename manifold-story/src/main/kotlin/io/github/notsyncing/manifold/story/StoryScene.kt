package io.github.notsyncing.manifold.story

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.story.vm.*
import io.github.notsyncing.manifold.suspendable.SuspendableScene
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future

open class StoryScene(private var sceneName: String,
                      noPersist: Boolean = false) : SuspendableScene<Any?>(noPersist) {
    private lateinit var interpreter: StoryInterpreter
    private lateinit var chapter: Chapter
    private var savedInterpreterState: InterpreterState? = null

    constructor() : this("")

    private fun loadScene() {
        chapter = StoryLibrary.get(sceneName)
        noPersist = chapter.noPersist

        if (chapter.type != ChapterType.Unknown) {
            interpreter = StoryInterpreter(this, chapter.content, chapter.labels)

            if (savedInterpreterState != null) {
                interpreter.state = savedInterpreterState!!
            }
        }
    }

    override fun serialize(): JSONObject {
        return JSONObject()
                .fluentPut("sceneName", sceneName)
                .fluentPut("interpreterState", JSON.toJSON(interpreter.state))
    }

    override fun deserialize(o: JSONObject) {
        sceneName = o.getString("sceneName")
        savedInterpreterState = JSON.parseObject(o.get("interpreterState").toString(), InterpreterState::class.java)
    }

    override fun stage() = future {
        super.stage().await()

        loadScene()

        val r = interpreter.run().await()

        if (r != null) {
            r
        } else {
            null
        }
    }
}