package io.github.notsyncing.manifold.management

import io.github.notsyncing.manifold.utils.removeIf
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class ManagementCommandManager {
    private val commandMap = ConcurrentHashMap<String, Method>()

    private val log = Logger.getLogger(javaClass.simpleName)

    fun addCommand(commandMethod: Method) {
        if (!commandMethod.isAnnotationPresent(ManagementCommand::class.java)) {
            return
        }

        if (!Modifier.isStatic(commandMethod.modifiers)) {
            return
        }

        var name = commandMethod.getAnnotation(ManagementCommand::class.java).value

        if (name.isEmpty()) {
            name = commandMethod.name
        }

        if (commandMap.containsKey(name)) {
            log.warning("Management command $name already registered, will be replaced!")
        }

        commandMap[name] = commandMethod
    }

    fun addCommands(commandClass: Class<*>) {
        if (!commandClass.isAnnotationPresent(ManagementCommands::class.java)) {
            return
        }

        log.info("Adding management commands from $commandClass")

        commandClass.methods
                .filter { it.isAnnotationPresent(ManagementCommand::class.java) }
                .forEach { addCommand(it) }
    }

    fun removeFromCacheIf(predicate: (String, Method) -> Boolean) {
        commandMap.removeIf { (name, method) -> predicate(name, method) }
    }

    fun getCommand(name: String): Method? {
        return commandMap[name]
    }
}