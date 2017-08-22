package io.github.notsyncing.manifold.story.drama

import jdk.nashorn.api.scripting.ScriptObjectMirror
import javax.script.CompiledScript

class DramaActionInfo(val name: String,
                      val permissionName: String,
                      val permissionType: String,
                      val code: ScriptObjectMirror,
                      val fromPath: String) {
}