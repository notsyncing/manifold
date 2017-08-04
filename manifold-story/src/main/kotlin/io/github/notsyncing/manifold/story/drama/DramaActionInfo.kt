package io.github.notsyncing.manifold.story.drama

import javax.script.CompiledScript

class DramaActionInfo(val name: String,
                      val permissionName: String,
                      val permissionType: String,
                      val code: CompiledScript,
                      val fromPath: String) {
}