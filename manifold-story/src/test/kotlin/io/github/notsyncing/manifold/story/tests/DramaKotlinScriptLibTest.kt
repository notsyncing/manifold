package io.github.notsyncing.manifold.story.tests

import com.alibaba.fastjson.JSON
import io.github.notsyncing.manifold.story.drama.engine.kotlin.expand
import io.github.notsyncing.manifold.story.tests.toys.TestNestedObject
import io.github.notsyncing.manifold.story.tests.toys.TestObject
import org.junit.Assert.assertEquals
import org.junit.Test

class DramaKotlinScriptLibTest {
    @Test
    fun testExpand() {
        val f = { obj1: TestObject, obj2: TestNestedObject, id: Long ->
            assertEquals("test1", obj1.name)
            assertEquals(1, obj1.id)

            assertEquals(2, obj2.count)
            assertEquals("test2", obj2.inner.name)
            assertEquals(3, obj2.inner.id)

            assertEquals(4, id)
        }

        val data = JSON.parseObject("""
            {"obj1":{"name":"test1","id":1},"obj2":{"count":2,"inner":{"name":"test2","id":3}},"id":4}
        """.trimIndent())

        expand(data, f)
    }

    @Test
    fun testExpandList() {
        val f = { arr: List<TestObject> ->
            assertEquals(3, arr.size)

            assertEquals(1, arr[0].id)
            assertEquals("test1", arr[0].name)

            assertEquals(2, arr[1].id)
            assertEquals("test2", arr[1].name)

            assertEquals(3, arr[2].id)
            assertEquals("test3", arr[2].name)
        }

        val data = JSON.parseObject("""
            {"arr":[{"name":"test1","id":1},{"name":"test2","id":2},{"name":"test3","id":3}]}
        """.trimIndent())

        expand(data, f)
    }
}