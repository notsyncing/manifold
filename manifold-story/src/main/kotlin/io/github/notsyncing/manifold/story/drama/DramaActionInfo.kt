package io.github.notsyncing.manifold.story.drama

import io.github.notsyncing.manifold.story.drama.engine.CallableObject

class DramaActionInfo(val name: String,
                      val permissionName: String? = null,
                      val permissionType: String? = null,
                      val code: CallableObject,
                      val fromPath: String,
                      val domain: String? = null,
                      val internal: Boolean = false) {
}