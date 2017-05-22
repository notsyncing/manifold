package io.github.notsyncing.manifold.story.vm

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ActionResult
import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.story.StoryScene
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.security.InvalidParameterException

class StoryInterpreter(private val scene: StoryScene,
                       private val content: List<Sentence>,
                       private val labels: Map<String, Sentence>) {
    var state = InterpreterState()

    fun run() = future<Any?> {
        var result: Any? = null

        while (!state.done) {
            val sentence = content[state.currentLine]
            var currentLineUpdated = false

            if (sentence.type != SentenceType.OpCode) {
                throw InvalidParameterException("Non-opcode sentence found: $sentence")
            }

            when (sentence.instruction) {
                Opcodes.Call -> {
                    val actionName = sentence.parameters[0]
                    val actionClass = Manifold.actionMetadata[actionName] ?: Class.forName(actionName)
                    val action = actionClass.newInstance() as ManifoldAction<*>

                    state.lastResult = scene.m(action).await()
                }

                Opcodes.Store -> {
                    val fromReg = sentence.parameters[0]
                    val toReg = sentence.parameters[1]

                    storeToRegister(toReg, loadFromRegister(fromReg))
                }

                Opcodes.WaitCall -> {
                    val actionName = sentence.parameters[0]
                    val resumeAtLabel = sentence.parameters[1]

                    val actionClass = (Manifold.actionMetadata[actionName] ?: Class.forName(actionName)) as Class<ManifoldAction<*>>
                    val oldCurrentLine = state.currentLine
                    state.currentLine = content.indexOf(labels[resumeAtLabel])

                    scene.awaitFor(actionClass)

                    state.currentLine = oldCurrentLine
                }

                Opcodes.Return -> {
                    val reg = sentence.parameters[0]
                    result = loadFromRegister(reg)

                    state.done = true
                }

                Opcodes.IfFailed -> {
                    val reg = sentence.parameters[0]
                    val toLabel = sentence.parameters[1]

                    val resultToCheck = loadFromRegister(reg) as ActionResult<*>

                    if (!resultToCheck.succeed) {
                        state.currentLine = content.indexOf(labels[toLabel])
                        currentLineUpdated = true
                    }
                }

                Opcodes.FinalReturn -> {
                    val reg = sentence.parameters[0]
                    result = loadFromRegister(reg)

                    state.done = true

                    scene.done()
                }
            }

            if (!currentLineUpdated) {
                state.currentLine++
            }
        }

        result
    }

    private fun loadFromRegister(reg: String): Any? {
        if (reg == Registers.LastResult) {
            return state.lastResult
        } else {
            val index = reg.substring(1).toInt()
            return state.registers[index]
        }
    }

    private fun storeToRegister(reg: String, value: Any?) {
        if (reg == Registers.LastResult) {
            state.lastResult = value
        } else {
            val index = reg.substring(1).toInt()
            state.registers[index] = value
        }
    }
}