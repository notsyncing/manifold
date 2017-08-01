package io.github.notsyncing.manifold.authenticate

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject

class AggregatedPermissions(val permissions: MutableList<Permission>) {
    constructor() : this(mutableListOf())

    fun get(module: Enum<*>, type: Enum<*>): Permission {
        return get(module.name, type.name)
    }

    fun get(module: String, type: String): Permission {
        val r = permissions.firstOrNull { (it.module == module) && (it.type == type) }

        if (r == null) {
            return Permission(module, type, PermissionState.Undefined)
        }

        return r
    }

    companion object {
        fun fromJson(json: JSONObject): AggregatedPermissions {
            val ap = AggregatedPermissions()
            val l = json.getJSONArray("permissions")

            for (o in l) {
                val p = o as JSONObject
                val pl = Permission(p.getIntValue("module"), p.getIntValue("type"),
                        PermissionState.values()[p.getIntValue("state")])

                ap.permissions.add(pl)

                if (p.containsKey("additionalData")) {
                    pl.additionalData = p["additionalData"]
                }

                if (p.containsKey("inherited")) {
                    pl.inherited = p.getBoolean("inherited")
                }
            }

            return ap
        }

        fun fromJson(json: String): AggregatedPermissions {
            return fromJson(JSON.parseObject(json))
        }
    }
}