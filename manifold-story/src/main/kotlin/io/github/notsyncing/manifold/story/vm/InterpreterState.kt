package io.github.notsyncing.manifold.story.vm

class InterpreterState(var currentLine: Int = 0,
                       val registers: Array<Any?> = arrayOfNulls(16),
                       var done: Boolean = false) {
}