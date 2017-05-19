package io.github.notsyncing.manifold.story.vm

import java.security.InvalidParameterException

class StoryInterpreter(private val content: List<Sentence>,
                       private val labels: Map<String, Sentence>) {
    fun run() {
        for (sentence in content) {
            if (sentence.type != SentenceType.OpCode) {
                throw InvalidParameterException("Non-opcode sentence found: $sentence")
            }

            when (sentence.instruction) {
                Opcodes.Call -> {

                }
            }
        }
    }
}