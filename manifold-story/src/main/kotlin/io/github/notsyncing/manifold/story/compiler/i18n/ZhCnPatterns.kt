package io.github.notsyncing.manifold.story.compiler.i18n

import io.github.notsyncing.manifold.story.compiler.ForLocale
import io.github.notsyncing.manifold.story.compiler.I18NPatterns

@ForLocale("zh-cn")
class ZhCnPatterns : I18NPatterns() {
    override val subTextSplitter: String
        get() = "，"
}