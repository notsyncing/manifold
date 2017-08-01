package io.github.notsyncing.manifold.authenticate

import com.alibaba.fastjson.JSONObject

class CustomPermissionInfo(val id: Int,
                           val name: String,
                           val parameters: JSONObject) {
}