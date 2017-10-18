package io.github.notsyncing.manifold.story.tests

import io.github.notsyncing.manifold.story.tests.toys.TestFunctions
import io.github.notsyncing.manifold.story.tests.toys.TestNestedObject
import io.github.notsyncing.manifold.story.tests.toys.TestObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class DramaNashornLibTest {
    private lateinit var engine: ScriptEngine

    @Before
    fun setUp() {
        engine = ScriptEngineManager().getEngineByName("nashorn")
        engine.eval("load('classpath:manifold_story/drama-lib-nashorn.js')")
    }

    @After
    fun tearDown() {

    }

    @Test
    fun testFromSimple() {
        val code = """
            var objClass = Java.type("${TestObject::class.java.name}");
            var obj = from(objClass, { id: 2, name: "test" });
        """.trimIndent()

        engine.eval(code)

        val result = engine.get("obj") as TestObject
        Assert.assertEquals(2, result.id)
        Assert.assertEquals("test", result.name)
    }

    @Test
    fun testFromNested() {
        val code = """
            var objClass = Java.type("${TestNestedObject::class.java.name}");
            var obj = from(objClass, { count: 3, inner: { id: 2, name: "test" } });
        """.trimIndent()

        engine.eval(code)

        val result = engine.get("obj") as TestNestedObject
        Assert.assertEquals(3, result.count)
        Assert.assertEquals(2, result.inner.id)
        Assert.assertEquals("test", result.inner.name)
    }

    @Test
    fun testToPromiseSuccess() {
        val code = """
            var TestFunctions = Java.type("${TestFunctions::class.java.name}");
            var result = "";

            toPromise(TestFunctions.successCf())
                .then(function (r) {
                    result = r;
                });
        """.trimIndent()

        engine.eval(code)

        Thread.sleep(100)

        val result = engine.get("result")
        Assert.assertEquals("SUCCESS", result)
    }

    @Test
    fun testToPromiseFailed() {
        val code = """
            var TestFunctions = Java.type("${TestFunctions::class.java.name}");
            var result = "";

            toPromise(TestFunctions.failedCf())
                .catch(function (err) {
                    result = err.message;
                });
        """.trimIndent()

        engine.eval(code)

        Thread.sleep(100)

        val result = engine.get("result")
        Assert.assertEquals("FAILED", result)
    }
}