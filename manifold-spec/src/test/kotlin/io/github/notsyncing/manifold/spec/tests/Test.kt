package io.github.notsyncing.manifold.spec.tests

import org.junit.Assert.assertTrue
import org.junit.Test

open class A(private val p: Boolean = false) {
    fun foo(): Boolean {
        return true
    }
}

typealias B = A

//class C : B(p = true) { -- ERROR
class C : A(p = true) {

}

class Test {
    @Test
    fun test() {
        val a = C()
        assertTrue(a.foo())
    }
}