package io.github.notsyncing.manifold.action

import com.alibaba.fastjson.JSON

open class SceneFailedException(val data: Any?) : Exception("Scene failed with data: ${JSON.toJSONString(data)}")