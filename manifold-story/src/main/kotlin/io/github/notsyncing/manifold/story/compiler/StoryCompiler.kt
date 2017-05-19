package io.github.notsyncing.manifold.story.compiler

import com.alibaba.fastjson.JSON
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.story.vm.Directives
import io.github.notsyncing.manifold.story.vm.Sentence
import io.github.notsyncing.manifold.story.vm.SentenceType
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.StringBufferInputStream
import java.util.stream.Collectors

class StoryCompiler {
    private val visitor = NodeVisitor(VisitHandler(Heading::class.java, this::visitHeading),
            VisitHandler(OrderedList::class.java, this::visitOrderedList),
            VisitHandler(OrderedListItem::class.java, this::visitOrderedListItem))

    private var currChapter = mutableListOf<Sentence>()
    private val chapters = mutableListOf<List<Sentence>>()
    private lateinit var metaInfo: StoryMetaInfo
    private lateinit var i18nPatterns: I18NPatterns

    private fun visitHeading(heading: Heading) {
        when (heading.level) {
            1, 2 -> {
                println("Compiler: in ${heading.text.unescape()}")
            }

            3 -> {
                if (currChapter.isNotEmpty()) {
                    currChapter.add(Sentence.directive(Directives.EndScene, emptyList()))
                    chapters.add(currChapter)
                }

                println("Compiler: in scene ${heading.text.unescape()}")

                currChapter = mutableListOf()

                val s = heading.text.unescape()
                val sceneName = s.substring(s.indexOf("-") + 1).trim()

                currChapter.add(Sentence.directive(Directives.Scene, listOf(sceneName)))
            }

            else -> {}
        }
    }

    private fun visitOrderedList(orderedList: OrderedList) {

    }

    private fun visitOrderedListItem(item: OrderedListItem) {
        println("Compiler: processing ${item.contentChars.unescape()}")

        val text = item.contentChars.unescape()
        val subText = text.split(i18nPatterns.subTextSplitter)


    }

    fun compile(input: String): String {
        val lines = input.lines()
        metaInfo = JSON.parseObject(lines[0], StoryMetaInfo::class.java)

        i18nPatterns = Manifold.dependencyProvider.getAllClassesImplemented(I18NPatterns::class.java)
                .first { it.getAnnotation(ForLocale::class.java)?.locale == metaInfo.locale }
                .newInstance()

        val data = lines.stream()
                .skip(1)
                .collect(Collectors.joining("\n"))

        val config = MutableDataSet()
        val parser = Parser.builder(config).build()
        val document = parser.parse(data)

        visitor.visit(document)

        return chapters.flatMap { it }
                .map { it.toString() }
                .joinToString("\n")
    }
}