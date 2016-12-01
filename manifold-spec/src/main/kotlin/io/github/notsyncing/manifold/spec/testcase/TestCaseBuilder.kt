package io.github.notsyncing.manifold.spec.testcase

class TestCaseBuilder {
    private val cases = mutableListOf<TestCaseInfo>()

    operator fun invoke(cases: TestCaseBuilder.() -> Unit) {
        this.cases()
    }

    infix fun String.on(caseInner: TestCaseInfo.() -> Unit): TestCaseInfo {
        val case = TestCaseInfo(this)
        case.caseInner()

        cases.add(case)
        return case
    }

    fun TestCaseInfo.given(conds: TestCaseInfo.() -> Unit): TestCaseInfo {
        this.conds()
        return this
    }

    fun TestCaseInfo.should(checks: TestCaseInfo.() -> Unit): TestCaseInfo {
        this.checks()
        return this
    }

    fun build(): List<TestCaseInfo> {
        return cases
    }
}