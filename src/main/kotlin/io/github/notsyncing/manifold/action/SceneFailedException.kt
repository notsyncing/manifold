package io.github.notsyncing.manifold.action

import com.alibaba.fastjson.JSON

open class SceneFailedException : Exception {
    val data: Any?

    constructor(data: Any?, cause: Throwable?) : super("Scene failed with data: ${JSON.toJSONString(data)}", cause) {
        this.data = data
    }

    constructor(data: Any?) : super("Scene failed with data: ${JSON.toJSONString(data)}") {
        this.data = data
    }
}