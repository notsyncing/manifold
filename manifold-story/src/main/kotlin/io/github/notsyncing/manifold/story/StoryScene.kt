package io.github.notsyncing.manifold.story

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.github.notsyncing.manifold.suspendable.SuspendableScene
import java.util.concurrent.CompletableFuture

open class StoryScene : SuspendableScene<JSON?>() {
    override fun serialize(): JSONObject {
        return JSONObject()
    }

    override fun deserialize(o: JSONObject) {
    }

    override fun stage(): CompletableFuture<JSON?> {
        return super.stage()
    }
}