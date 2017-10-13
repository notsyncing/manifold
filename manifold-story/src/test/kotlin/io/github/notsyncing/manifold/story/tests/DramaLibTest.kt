package io.github.notsyncing.manifold.story.tests

import io.github.notsyncing.manifold.story.tests.toys.TestNestedObject
import io.github.notsyncing.manifold.story.tests.toys.TestObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class DramaLibTest {
    private lateinit var engine: ScriptEngine

    @Before
    fun setUp() {
        engine = ScriptEngineManager().getEngineByName("nashorn")
        engine.eval("load('classpath:manifold_story/drama-lib.js')")
    }

    @After
    fun tearDown() {

    }

    @Test
    fun testFillSimple() {
        val code = """
            var objClass = Java.type("${TestObject::class.java.name}");
            var obj = new objClass();
            fill(obj, { id: 2, name: "test" });
        """.trimIndent()

        engine.eval(code)

        val result = engine.get("obj") as TestObject
        Assert.assertEquals(2, result.id)
        Assert.assertEquals("test", result.name)
    }

    @Test
    fun testFillNested() {
        val code = """
            var objClass = Java.type("${TestNestedObject::class.java.name}");
            var obj = new objClass();
            fill(obj, { count: 3, inner: { id: 2, name: "test" } });
        """.trimIndent()

        engine.eval(code)

        val result = engine.get("obj") as TestNestedObject
        Assert.assertEquals(3, result.count)
        Assert.assertEquals(2, result.inner.id)
        Assert.assertEquals("test", result.inner.name)
    }
}