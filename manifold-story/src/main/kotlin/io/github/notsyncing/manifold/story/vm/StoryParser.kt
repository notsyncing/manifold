package io.github.notsyncing.manifold.story.vm

import java.io.BufferedInputStream
import java.io.InputStream
import java.text.ParseException

class StoryParser {
    companion object {
        const val CURRENT_VERSION = 1

        fun parse(data: InputStream) = StoryParser().parse(data)
    }

    private val errors = mutableListOf<String>()
    private val _chapters = mutableListOf<Chapter>()

    val chapters get() = _chapters.toList()

    fun parse(data: InputStream): StoryParser {
        BufferedInputStream(data).bufferedReader().useLines {
            var currChapter: Chapter? = null
            var currLabel: String? = null

            it.forEach {
                val sentence: Sentence

                try {
                    sentence = Sentence.parse(it) ?: return@forEach
                } catch (e: ParseException) {
                    errors.add(e.message ?: "")
                    return@forEach
                }

                if (sentence.type == SentenceType.Directive) {
                    when (sentence.instruction) {
                        Directives.Version -> {
                            if (sentence.parameters[0] != CURRENT_VERSION.toString()) {
                                errors.add("Incompatible version ${sentence.parameters[0]}, current version $CURRENT_VERSION")
                            }
                        }

                        Directives.Scene -> {
                            currChapter = Chapter(sentence.parameters[0], ChapterType.Scene)
                        }

                        Directives.EndScene -> {
                            _chapters.add(currChapter!!)
                            currChapter = null
                        }

                        Directives.Label -> {
                            currLabel = sentence.parameters[0]
                        }

                        Directives.NoPersist -> {
                            currChapter!!.noPersist = true
                        }

                        else -> {
                            errors.add("Unknown directive in sentence $sentence")
                        }
                    }
                } else if (sentence.type == SentenceType.OpCode) {
                    if (currLabel != null) {
                        currChapter!!.labels[currLabel!!] = sentence
                        currLabel = null
                    }

                    currChapter!!.content.add(sentence)
                } else {
                    errors.add("Unknown sentence $sentence")
                }
            }
        }

        return this
    }

    fun hasError() = errors.size > 0
}