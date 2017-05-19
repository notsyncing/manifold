package io.github.notsyncing.manifold.story.vm

import java.text.ParseException

open class Sentence(val type: SentenceType,
                    val instruction: String,
                    val parameters: List<String>) {
    companion object {
        fun parse(str: String?): Sentence? {
            if ((str == null) || (str.isNullOrBlank())) {
                return null
            }

            val s = str.trim()
            val parameters = mutableListOf<String>()
            val splitterIndex = s.indexOf(" ")

            if (splitterIndex <= 0) {
                throw ParseException("No splitter found in sentence $str", 0)
            }

            val instruction = s.substring(0, splitterIndex)
            val paramStr = s.substring(splitterIndex + 1)

            if (instruction.isBlank()) {
                throw ParseException("No instruction found in sentence $str", 0)
            }

            val type: SentenceType

            if (instruction.startsWith(".")) {
                type = SentenceType.Directive
            } else {
                type = SentenceType.OpCode
            }

            paramStr.split(",").forEach { parameters.add(it.trim()) }

            return Sentence(type, instruction, parameters)
        }
    }

    override fun toString(): String {
        return "$instruction ${parameters.joinToString(", ")}"
    }
}