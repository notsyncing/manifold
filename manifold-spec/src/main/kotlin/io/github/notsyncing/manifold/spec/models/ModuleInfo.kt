package io.github.notsyncing.manifold.spec.models

import io.github.notsyncing.manifold.spec.SpecSceneGroup

class ModuleInfo(val name: String,
                 val children: MutableList<ModuleInfo> = mutableListOf(),
                 val sceneGroups: MutableList<SpecSceneGroup> = mutableListOf()) {
}