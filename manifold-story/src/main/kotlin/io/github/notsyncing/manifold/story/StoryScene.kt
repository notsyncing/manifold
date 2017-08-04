package io.github.notsyncing.manifold.story

import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.suspendable.SuspendableScene
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future

open class StoryScene(private var sceneName: String,
                      noPersist: Boolean = false) : SuspendableScene<Any?>(noPersist) {

    constructor() : this("")

    override fun serialize(): JSONObject {
        return JSONObject()
                .fluentPut("sceneName", sceneName)
    }

    override fun deserialize(o: JSONObject) {
        sceneName = o.getString("sceneName")
    }

    override fun stage() = future {
        super.stage().await()
    }
}