package io.github.notsyncing.manifold.tests

import io.github.notsyncing.manifold.mapping.fillFrom
import io.github.notsyncing.manifold.mapping.mapAs
import io.github.notsyncing.manifold.tests.toys.mapping.A
import io.github.notsyncing.manifold.tests.toys.mapping.B
import io.github.notsyncing.manifold.tests.toys.mapping.SubA
import org.junit.Assert.assertEquals
import org.junit.Test

class MappingTest {
    @Test
    fun testSimpleMapping() {
        val a = A()
        a.id = 1
        a.name = "test"

        val subA = a.mapAs<SubA>()
        assertEquals(1, subA.id)
        assertEquals("test", subA.name)
        assertEquals("", subA.title)
    }

    @Test
    fun testSimpleMappingList() {
        val list = listOf(
                A().apply {
                    id = 1
                    name = "test1"
                },
                A().apply {
                    id = 2
                    name = "test2"
                },
                A().apply {
                    id = 3
                    name = "test3"
                })

        val subList = list.mapAs<SubA>()

        assertEquals(3, subList.size)
        assertEquals(1, subList[0].id)
        assertEquals("test1", subList[0].name)
        assertEquals(2, subList[1].id)
        assertEquals("test2", subList[1].name)
        assertEquals(3, subList[2].id)
        assertEquals("test3", subList[2].name)
    }

    @Test
    fun testSimpleFilling() {
        val a = SubA()
        a.id = 2
        a.title = "testTitle"
        a.name = "testName"

        val b = B()
        b.code = 3
        b.title = "testNewTitle"

        a.fillFrom(b)

        assertEquals(2, a.id)
        assertEquals("testName", a.name)
        assertEquals("testNewTitle", a.title)
    }

    @Test
    fun testSimpleFillingList() {
        val list = listOf(
                SubA().apply {
                    id = 1
                    name = "test1"
                    title = "title1"
                },
                SubA().apply {
                    id = 2
                    name = "test2"
                    title = "title2"
                },
                SubA().apply {
                    id = 3
                    name = "test3"
                    title = "title3"
                })

        val list2 = listOf(
                B().apply {
                    code = 4
                    title = "title4"
                },
                B().apply {
                    code = 5
                    title = "title5"
                },
                B().apply {
                    code = 6
                    title = "title6"
                })

        list.fillFrom(list2)

        assertEquals("title4", list[0].title)
        assertEquals("title5", list[1].title)
        assertEquals("title6", list[2].title)
    }

    @Test
    fun testSimpleFillingSkip() {
        val a = SubA()
        a.id = 2
        a.title = "testTitle"
        a.name = "testName"

        val b = B()
        b.code = 3
        b.title = "testNewTitle"

        a.fillFrom(b, skipFields = setOf(b::title))

        assertEquals(2, a.id)
        assertEquals("testName", a.name)
        assertEquals("testTitle", a.title)
    }

    @Test
    fun testSimpleFillingListMatching() {
        val list = listOf(
                SubA().apply {
                    id = 1
                    name = "test1"
                    title = "title1"
                },
                SubA().apply {
                    id = 2
                    name = "test2"
                    title = "title2"
                },
                SubA().apply {
                    id = 3
                    name = "test3"
                    title = "title3"
                })

        val list2 = listOf(
                B().apply {
                    code = 3
                    title = "title4"
                },
                B().apply {
                    code = 1
                    title = "title5"
                },
                B().apply {
                    code = 2
                    title = "title6"
                })

        list.fillFrom(list2, matching = { f, t -> f.code.toLong() == t.id })

        assertEquals("title5", list[0].title)
        assertEquals("title6", list[1].title)
        assertEquals("title4", list[2].title)
    }
}