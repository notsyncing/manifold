package io.github.notsyncing.manifold.tests

import kotlinx.coroutines.async
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CompletableFuture

class Action<T> {
    fun execute(f: CompletableFuture<T>) = async<T> {
        val r = await(f)

        await(CompletableFuture.completedFuture(null))

        return@async r
    }
}

class ActionS {
    fun execute(f: CompletableFuture<String>) = async<String> {
        val r = await(f)

        await(CompletableFuture.completedFuture(null))

        return@async r
    }
}

class TestClass {
    @Test
    fun test() {
        val q = Action<String>().execute(CompletableFuture.completedFuture("Test")).get()

        Assert.assertEquals("Test", q)
    }

    @Test
    fun testS() {
        val q = ActionS().execute(CompletableFuture.completedFuture("TestS")).get()

        Assert.assertEquals("TestS", q)
    }
}

