package io.github.notsyncing.manifold.story.drama

interface DramaScriptLifecycle {
    fun afterEvaluate()
    fun beforeDestroy()
}