package io.github.notsyncing.manifold.rules

import io.github.notsyncing.manifold.management.ManagementCommand
import io.github.notsyncing.manifold.management.ManagementCommands

@ManagementCommands
object RuleManagementCommands {
    @ManagementCommand
    @JvmStatic
    fun ruleUpdated(name: String): String {
        RuleEngine.ruleChanged(name)
        return "OK"
    }
}