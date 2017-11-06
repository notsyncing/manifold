package io.github.notsyncing.manifold.story.drama.engine

import io.github.notsyncing.manifold.story.drama.engine.kotlin.KotlinScriptDramaEngine
import io.github.notsyncing.manifold.story.drama.engine.nashorn.NashornDramaEngine

object DramaEngineFactory {
    private var nashorn: NashornDramaEngine? = null
    private var kotlin: KotlinScriptDramaEngine? = null

    fun getByFile(path: String): DramaEngine {
        if (path.endsWith(NashornDramaEngine.DRAMA_EXT)) {
            if (nashorn == null) {
                nashorn = NashornDramaEngine()
            }

            return nashorn!!
        } else if (path.endsWith(KotlinScriptDramaEngine.DRAMA_EXT)) {
            if (kotlin == null) {
                kotlin = KotlinScriptDramaEngine()
            }

            return kotlin!!
        }

        throw UnsupportedOperationException("Unsupported drama file $path")
    }

    fun isSupportedFile(path: String): Boolean {
        return (path.endsWith(NashornDramaEngine.DRAMA_EXT))
                || (path.endsWith(KotlinScriptDramaEngine.DRAMA_EXT))
    }
}