package io.github.notsyncing.manifold.story.drama.engine

object DramaEngineFactory {
    private var nashorn: NashornDramaEngine? = null

    fun getByFile(path: String): DramaEngine {
        if (path.endsWith(NashornDramaEngine.DRAMA_EXT)) {
            if (nashorn == null) {
                nashorn = NashornDramaEngine()
            }

            return nashorn!!
        }

        throw UnsupportedOperationException("Unsupported drama file $path")
    }

    fun isSupportedFile(path: String): Boolean {
        return path.endsWith(NashornDramaEngine.DRAMA_EXT)
    }
}