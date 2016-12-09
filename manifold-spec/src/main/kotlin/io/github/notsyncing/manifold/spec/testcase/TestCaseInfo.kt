package io.github.notsyncing.manifold.spec.testcase

import io.github.notsyncing.manifold.spec.models.Ref

class TestCaseInfo(val behavior: String) {
    val sessionIdentifier = "manifold.session.identifier"

    class TestCaseExitPoint {
        var exitName: String = ""
        var result: Any? = null
        var hasResult = false
        var resultInto: Ref<Any?>? = null

        infix fun at(exitName: String): TestCaseExitPoint {
            this.exitName = exitName
            return this
        }

        infix fun with(result: Any?): TestCaseExitPoint {
            hasResult = true
            this.result = result
            return this
        }

        infix fun resultInto(variable: Ref<*>): TestCaseExitPoint {
            resultInto = variable as Ref<Any?>
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