package io.github.notsyncing.manifold.story.compiler.i18n

import io.github.notsyncing.manifold.story.compiler.ForLocale
import io.github.notsyncing.manifold.story.compiler.I18NPatterns
import io.github.notsyncing.manifold.story.vm.Sentence

@ForLocale("zh-cn")
class ZhCnPatterns : I18NPatterns() {
    override val subTextSplitter: String
        get() = "，"

    override fun processSubText(subText: String): Sentence? {


        return null
    }
}