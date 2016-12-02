package io.github.notsyncing.manifold.spec.testcase

class TestCaseBuilder {
    private val cases = mutableListOf<TestCaseInfo>()

    operator fun invoke(cases: TestCaseBuilder.() -> Unit) {
        this.cases()
    }

    operator fun String.invoke(caseInner: TestCaseInfo.() -> Unit): TestCaseInfo {
        val case = TestCaseInfo(this)
        case.caseInner()

        cases.add(case)
        return case
    }

    fun build(): List<TestCaseInfo> {
        return cases
    }
}