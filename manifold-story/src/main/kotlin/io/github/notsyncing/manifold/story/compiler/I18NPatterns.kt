package io.github.notsyncing.manifold.story.compiler

import io.github.notsyncing.manifold.story.vm.Sentence

abstract class I18NPatterns {
    abstract val subTextSplitter: String

    abstract fun processSubText(subText: String): Sentence?
}