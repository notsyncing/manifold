package io.github.notsyncing.manifold.spec.testcase

class TestCaseInfo(val behavior: String) {
    class TestCaseExitPoint {
        var exitName: String = ""

        infix fun at(exitName: String): TestCaseExitPoint {
            this.exitName = exitName
            return this
        }
    }

    val parameters = mutableMapOf<String, String?>()
    val exit = TestCaseExitPoint()
    val additionalConditions = mutableListOf<() -> Boolean>()

    infix fun String?.into(variable: String) {
        parameters[variable] = this
    }

    fun and(cond: () -> Boolean): TestCaseInfo {
        additionalConditions.add(cond)
        return this
    }
}