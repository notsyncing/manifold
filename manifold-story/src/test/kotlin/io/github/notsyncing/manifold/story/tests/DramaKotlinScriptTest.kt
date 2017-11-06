package io.github.notsyncing.manifold.story.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.authenticate.*
import io.github.notsyncing.manifold.story.drama.DramaManager
import io.github.notsyncing.manifold.story.drama.DramaScene
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture

class DramaKotlinScriptTest {
    @Before
    fun setUp() {
        Manifold.enableFeatureManagement = false

        Manifold.init()

        DramaManager.init()
    }

    @After
    fun tearDown() {
        DramaManager.destroy()
        DramaManager.reset()

        Manifold.destroy().get()
    }

    @Test
    fun testSimpleDrama() {
        val r = Manifold.run(DramaScene("simpleActionKt")).get()
        Assert.assertEquals("Hello, world!", r)
    }

    @Test
    fun testSimpleDramaWithPermissions() {
        Manifold.authInfoProvider = object : AuthenticateInformationProvider {
            override fun getRole(id: String): CompletableFuture<AuthRole?> {
                return CompletableFuture.completedFuture(AuthRole(1, "", emptyArray(),
                        arrayOf(Permission("TestModule", "TestAuth", PermissionState.Allowed))))
            }
        }

        val r = Manifold.run(DramaScene("simpleAuthActionKt"), "someId").get()
        Assert.assertEquals("Hello, user!", r)
    }

    @Test
    fun testSimpleDramaWithPermissionsNotPassed() {
        Manifold.authInfoProvider = object : AuthenticateInformationProvider {
            override fun getRole(id: String): CompletableFuture<AuthRole?> {
                return CompletableFuture.completedFuture(AuthRole(1, "", emptyArray(),
                        arrayOf(Permission("A", "B", PermissionState.Allowed))))
            }
        }

        try {
            val r = Manifold.run(DramaScene("simpleAuthActionKt"), "someId").get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is NoPermissionException)
        }
    }

    @Test
    fun testSimpleDramaWithPermissionsNoRole() {
        try {
            val r = Manifold.run(DramaScene("simpleAuthActionKt")).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is NoPermissionException)
        }
    }

    @Test
    fun testDramaWithExceptionInKotlin() {
        try {
            Manifold.run(DramaScene("exceptionInKotlinActionKt")).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertEquals("EXCEPTION", e.cause?.cause?.message)
        }
    }

    @Test
    fun testDramaWithExceptionInJava() {
        try {
            Manifold.run(DramaScene("exceptionInJavaActionKt")).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertEquals("EXCEPTION", e.cause?.cause?.message)
        }
    }

    @Test
    fun testNestedCFPWithExceptionInJava() {
        try {
            val r = Manifold.run(DramaScene("nestedCFPAction1Kt")).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertEquals("FAILED", e.cause?.message)
        }
    }
}