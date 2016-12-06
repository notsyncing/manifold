package io.github.notsyncing.manifold.spec.testcase

class TestCaseInfo(val behavior: String) {
    val sessionIdentifier = "manifold.session.identifier"

    class TestCaseExitPoint {
        var exitName: String = ""
        var result: Any? = null

        infix fun at(exitName: String): TestCaseExitPoint {
            this.exitName = exitName
            return this
        }

        infix fun with(result: Any?): TestCaseExitPoint {
            this.result = result
            return this
        }
    }

    class TestAdditionalCondition(val name: String, val cond: () -> Boolean) {

    }

    val parameters = mutableMapOf<String, Any?>()
    var otherInit: (() -> Unit)? = null
    val exit = TestCaseExitPoint()
    val additionalConditions = mutableListOf<TestAdditionalCondition>()

    infix fun Any?.into(variable: String) {
        parameters[variable] = this
    }

    fun and(name: String, cond: () -> Boolean): TestCaseInfo {
        additionalConditions.add(TestAdditionalCondition(name, cond))
        return this
    }

    fun given(conds: TestCaseInfo.() -> Unit): TestCaseInfo {
        this.conds()
        return this
    }

    fun others(proc: () -> Unit): TestCaseInfo {
        otherInit = proc
        return this
    }

    fun should(checks: TestCaseInfo.() -> Unit): TestCaseInfo {
        this.checks()
        return this
    }
}