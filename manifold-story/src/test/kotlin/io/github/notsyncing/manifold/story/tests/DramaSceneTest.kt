package io.github.notsyncing.manifold.story.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.authenticate.*
import io.github.notsyncing.manifold.story.drama.DramaManager
import io.github.notsyncing.manifold.story.drama.DramaScene
import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.CompletableFuture

class DramaSceneTest {
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
        val r = Manifold.run(DramaScene("simpleAction")).get()
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

        val r = Manifold.run(DramaScene("simpleAuthAction"), "someId").get()
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
            val r = Manifold.run(DramaScene("simpleAuthAction"), "someId").get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is NoPermissionException)
        }
    }

    @Test
    fun testSimpleDramaWithPermissionsNoRole() {
        try {
            val r = Manifold.run(DramaScene("simpleAuthAction")).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is NoPermissionException)
        }
    }

    @Test
    fun testSimpleDramaFromExternalFile() {
        val tmpFile = Files.createTempFile("manifold_drama_test_", ".drama.js")

        try {
            javaClass.getResourceAsStream("/manifold_test_dramas/simple.txt").use { inputStream ->
                Files.newOutputStream(tmpFile).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }

            DramaManager.addDramaSearchPath(tmpFile.parent)

            val r = Manifold.run(DramaScene("simpleActionExternal")).get()
            Assert.assertEquals("Hello, world!", r)
        } finally {
            Files.deleteIfExists(tmpFile)
        }
    }

    @Test
    fun testSimpleDramaFromExternalFileChanged() {
        val tmpDir = Files.createTempDirectory("manifold_drama_test_")
        val tmpFile = Files.createFile(tmpDir.resolve("test.drama.js"))

        try {
            javaClass.getResourceAsStream("/manifold_test_dramas/simple.txt").use { inputStream ->
                Files.newOutputStream(tmpFile).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }

            DramaManager.addDramaSearchPath(tmpDir)

            var r = Manifold.run(DramaScene("simpleActionExternal")).get()
            Assert.assertEquals("Hello, world!", r)

            Thread.sleep(2000)

            javaClass.getResourceAsStream("/manifold_test_dramas/simple2.txt").use { inputStream ->
                Files.newOutputStream(tmpFile).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }

            Thread.sleep(5000)

            r = Manifold.run(DramaScene("simpleActionExternal")).get()
            Assert.assertEquals("Hello, world 2!", r)
        } finally {
            Files.delete(tmpFile)
            Files.delete(tmpDir)
        }
    }

    @Test
    fun testSimpleDramaFromExternalFileDeleted() {
        val tmpDir = Files.createTempDirectory("manifold_drama_test_")
        val tmpFile = Files.createFile(tmpDir.resolve("test.drama.js"))

        try {
            javaClass.getResourceAsStream("/manifold_test_dramas/simple.txt").use { inputStream ->
                Files.newOutputStream(tmpFile).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }

            DramaManager.addDramaSearchPath(tmpDir)

            var r = Manifold.run(DramaScene("simpleActionExternal")).get()
            Assert.assertEquals("Hello, world!", r)

            Thread.sleep(2000)

            Files.delete(tmpFile)

            Thread.sleep(5000)

            try {
                r = Manifold.run(DramaScene("simpleActionExternal")).get()
                Assert.assertTrue(false)
            } catch (e: Exception) {
                Assert.assertTrue(e.cause is ClassNotFoundException)
            }
        } finally {
            Files.deleteIfExists(tmpFile)
            Files.delete(tmpDir)
        }
    }

    @Test
    fun testSimpleDramaFromExternalFileOverrideClasspath() {
        var r = Manifold.run(DramaScene("simpleAction")).get()
        Assert.assertEquals("Hello, world!", r)

        val tmpDir = Files.createTempDirectory("manifold_drama_test_")
        val tmpFile = Files.createFile(tmpDir.resolve("test.drama.js"))

        try {
            javaClass.getResourceAsStream("/manifold_test_dramas/simple3.txt").use { inputStream ->
                Files.newOutputStream(tmpFile).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }

            DramaManager.addDramaSearchPath(tmpDir)

            r = Manifold.run(DramaScene("simpleAction")).get()
            Assert.assertEquals("Hello, world, overrided!", r)
        } finally {
            Files.deleteIfExists(tmpFile)
            Files.delete(tmpDir)
        }
    }

    @Test
    fun testDramaWithExceptionInJs() {
        try {
            Manifold.run(DramaScene("exceptionInJsAction")).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertEquals("Error: EXCEPTION", e.cause?.message)
        }
    }

    @Test
    fun testDramaWithExceptionInJava() {
        try {
            Manifold.run(DramaScene("exceptionInJavaAction")).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertEquals("java.lang.Exception: EXCEPTION", e.cause?.message)
        }
    }

    @Test
    fun testNestedCFPWithExceptionInJava() {
        try {
            val r = Manifold.run(DramaScene("nestedCFPAction1")).get()
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertEquals("FAILED", e.cause?.message)
        }
    }
}