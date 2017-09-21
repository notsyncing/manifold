package io.github.notsyncing.manifold.story.drama

import jdk.nashorn.api.scripting.ScriptObjectMirror
import javax.script.CompiledScript

class DramaActionInfo(val name: String,
                      val permissionName: String? = null,
                      val permissionType: String? = null,
                      val code: ScriptObjectMirror,
                      val fromPath: String,
                      val domain: String? = null) {
}