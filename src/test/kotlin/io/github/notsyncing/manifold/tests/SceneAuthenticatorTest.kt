package io.github.notsyncing.manifold.tests

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldActionContextRunner
import io.github.notsyncing.manifold.action.interceptors.InterceptorResult
import io.github.notsyncing.manifold.action.interceptors.SceneInterceptorContext
import io.github.notsyncing.manifold.authenticate.*
import io.github.notsyncing.manifold.tests.toys.TestSceneSimple
import io.github.notsyncing.manifold.tests.toys.authenticate.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture

class SceneAuthenticatorTest {
    @Before
    fun setUp() {
        Manifold.authInfoProvider = object : AuthenticateInformationProvider {
            override fun getRole(id: String): CompletableFuture<AuthRole?> {
                if (id == "simple") {
                    val p = arrayOf(Permission(TestAuthModule.ModuleA, TestAuthType.Read, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleA, TestAuthType.Write, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Read, PermissionState.Forbidden))
                    val r = AuthRole(0, permissions = p)
                    return CompletableFuture.completedFuture(r)
                } else if (id == "one_group") {
                    val pg = arrayOf(Permission(TestAuthModule.ModuleA, TestAuthType.Write, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Read, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Write, PermissionState.Allowed))
                    val g = AuthGroup(permissions = pg)

                    val p = arrayOf(Permission(TestAuthModule.ModuleA, TestAuthType.Read, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleA, TestAuthType.Write, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Read, PermissionState.Forbidden))
                    val r = AuthRole(0, groups = arrayOf(g), permissions = p)
                    return CompletableFuture.completedFuture(r)
                } else if (id == "two_higher_groups") {
                    val pg2 = arrayOf(Permission(TestAuthModule.ModuleC, TestAuthType.Write, PermissionState.Allowed))
                    val g2 = AuthGroup(permissions = pg2)

                    val pg = arrayOf(Permission(TestAuthModule.ModuleA, TestAuthType.Write, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Read, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Write, PermissionState.Allowed))
                    val g = AuthGroup(parentGroup = g2, permissions = pg)

                    val p = arrayOf(Permission(TestAuthModule.ModuleA, TestAuthType.Read, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleA, TestAuthType.Write, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Read, PermissionState.Forbidden))
                    val r = AuthRole(0, groups = arrayOf(g), permissions = p)
                    return CompletableFuture.completedFuture(r)
                } else if (id == "two_branch_groups") {
                    val pg2 = arrayOf(Permission(TestAuthModule.ModuleC, TestAuthType.Write, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Write, PermissionState.Forbidden))
                    val g2 = AuthGroup(permissions = pg2)

                    val pg = arrayOf(Permission(TestAuthModule.ModuleA, TestAuthType.Write, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Read, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Write, PermissionState.Allowed))
                    val g = AuthGroup(permissions = pg)

                    val p = arrayOf(Permission(TestAuthModule.ModuleA, TestAuthType.Read, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleA, TestAuthType.Write, PermissionState.Allowed),
                            Permission(TestAuthModule.ModuleB, TestAuthType.Read, PermissionState.Forbidden))
                    val r = AuthRole(0, groups = arrayOf(g, g2), permissions = p)
                    return CompletableFuture.completedFuture(r)
                } else {
                    return CompletableFuture.completedFuture(AuthRole(0, "", emptyArray(), emptyArray()))
                }
            }
        }

        SceneAuthenticator.reset()
        TestSceneAuthenticator.reset()
        TestSceneAuthenticatorWithAgg.reset()
    }

    private fun execute(id: String, module: TestAuthModule, t: TestAuthType, isAgg: Boolean = false): SceneInterceptorContext {
        val anno = mock<TestAuthAnno> {
            on { value } doReturn module
            on { type } doReturn t
        }

        val scene = TestSceneSimple()
        scene.context.sessionIdentifier = id
        scene.m = ManifoldActionContextRunner(id, scene.context)

        val context = SceneInterceptorContext(scene)
        context.annotation = anno

        val auth = if (isAgg) TestSceneAuthenticatorWithAgg() else TestSceneAuthenticator()
        auth.before(context).get()

        return context
    }

    @Test
    fun testAuthPass() {
        val context = execute("simple", TestAuthModule.ModuleA, TestAuthType.Read)

        Assert.assertEquals(InterceptorResult.Continue, context.interceptorResult)
        Assert.assertNull(context.exception)
        Assert.assertEquals(TestAuthModule.ModuleA.name, TestSceneAuthenticator.permission!!.module)
        Assert.assertEquals(TestAuthType.Read.name, TestSceneAuthenticator.permission!!.type)
        Assert.assertEquals(PermissionState.Allowed, TestSceneAuthenticator.permission!!.state)
    }

    @Test
    fun testAuthPassWithAgg() {
        val context = execute("simple", TestAuthModule.ModuleA, TestAuthType.Read, isAgg = true)

        Assert.assertEquals(InterceptorResult.Continue, context.interceptorResult)
        Assert.assertNull(context.exception)
        Assert.assertEquals(TestAuthModule.ModuleA.name, TestSceneAuthenticatorWithAgg.permission!!.module)
        Assert.assertEquals(TestAuthType.Read.name, TestSceneAuthenticatorWithAgg.permission!!.type)
        Assert.assertEquals(PermissionState.Allowed, TestSceneAuthenticatorWithAgg.permission!!.state)
    }

    @Test
    fun testAuthPassOneGroup() {
        val context = execute("one_group", TestAuthModule.ModuleA, TestAuthType.Read)

        Assert.assertEquals(InterceptorResult.Continue, context.interceptorResult)
        Assert.assertNull(context.exception)
        Assert.assertEquals(TestAuthModule.ModuleA.name, TestSceneAuthenticator.permission!!.module)
        Assert.assertEquals(TestAuthType.Read.name, TestSceneAuthenticator.permission!!.type)
        Assert.assertEquals(PermissionState.Allowed, TestSceneAuthenticator.permission!!.state)
    }

    @Test
    fun testAuthPassOneGroupWithAgg() {
        val context = execute("one_group", TestAuthModule.ModuleA, TestAuthType.Read, isAgg = true)

        Assert.assertEquals(InterceptorResult.Continue, context.interceptorResult)
        Assert.assertNull(context.exception)
        Assert.assertEquals(TestAuthModule.ModuleA.name, TestSceneAuthenticatorWithAgg.permission!!.module)
        Assert.assertEquals(TestAuthType.Read.name, TestSceneAuthenticatorWithAgg.permission!!.type)
        Assert.assertEquals(PermissionState.Allowed, TestSceneAuthenticatorWithAgg.permission!!.state)

        val p = context.sceneContext.permissions
        Assert.assertNotNull(p)
        Assert.assertEquals(4, p!!.permissions.size)
        Assert.assertEquals(TestAuthModule.ModuleA.name, p.permissions[0].module)
        Assert.assertEquals(TestAuthType.Read.name, p.permissions[0].type)
        Assert.assertEquals(PermissionState.Allowed, p.permissions[0].state)

        Assert.assertEquals(TestAuthModule.ModuleA.name, p.permissions[1].module)
        Assert.assertEquals(TestAuthType.Write.name, p.permissions[1].type)
        Assert.assertEquals(PermissionState.Allowed, p.permissions[1].state)

        Assert.assertEquals(TestAuthModule.ModuleB.name, p.permissions[2].module)
        Assert.assertEquals(TestAuthType.Read.name, p.permissions[2].type)
        Assert.assertEquals(PermissionState.Forbidden, p.permissions[2].state)

        Assert.assertEquals(TestAuthModule.ModuleB.name, p.permissions[3].module)
        Assert.assertEquals(TestAuthType.Write.name, p.permissions[3].type)
        Assert.assertEquals(PermissionState.Allowed, p.permissions[3].state)
    }

    @Test
    fun testAuthNoPassWithForbidden() {
        val context = execute("simple", TestAuthModule.ModuleB, TestAuthType.Read)

        Assert.assertEquals(InterceptorResult.Stop, context.interceptorResult)
        Assert.assertNotNull(context.exception)
        Assert.assertTrue(context.exception is NoPermissionException)
        Assert.assertEquals(TestAuthModule.ModuleB.name, TestSceneAuthenticator.permission!!.module)
        Assert.assertEquals(TestAuthType.Read.name, TestSceneAuthenticator.permission!!.type)
        Assert.assertEquals(PermissionState.Forbidden, TestSceneAuthenticator.permission!!.state)
    }

    @Test
    fun testAuthNoPassWithForbiddenWithAgg() {
        val context = execute("simple", TestAuthModule.ModuleB, TestAuthType.Read, isAgg = true)

        Assert.assertEquals(InterceptorResult.Stop, context.interceptorResult)
        Assert.assertNotNull(context.exception)
        Assert.assertTrue(context.exception is NoPermissionException)
        Assert.assertEquals(TestAuthModule.ModuleB.name, TestSceneAuthenticatorWithAgg.permission!!.module)
        Assert.assertEquals(TestAuthType.Read.name, TestSceneAuthenticatorWithAgg.permission!!.type)
        Assert.assertEquals(PermissionState.Forbidden, TestSceneAuthenticatorWithAgg.permission!!.state)
    }

    @Test
    fun testAuthNoPassWithUndefined() {
        SceneAuthenticator.denyUndefinedPermissions = true
        val context = execute("simple", TestAuthModule.ModuleB, TestAuthType.Write)

        Assert.assertEquals(InterceptorResult.Stop, context.interceptorResult)
        Assert.assertNotNull(context.exception)
        Assert.assertTrue(context.exception is NoPermissionException)
        Assert.assertEquals(TestAuthModule.ModuleB.name, TestSceneAuthenticator.permission!!.module)
        Assert.assertEquals(TestAuthType.Write.name, TestSceneAuthenticator.permission!!.type)
        Assert.assertEquals(PermissionState.Undefined, TestSceneAuthenticator.permission!!.state)
    }

    @Test
    fun testAuthNoPassWithUndefinedWithAgg() {
        SceneAuthenticator.denyUndefinedPermissions = true
        val context = execute("simple", TestAuthModule.ModuleB, TestAuthType.Write, isAgg = true)

        Assert.assertEquals(InterceptorResult.Stop, context.interceptorResult)
        Assert.assertNotNull(context.exception)
        Assert.assertTrue(context.exception is NoPermissionException)
        Assert.assertEquals(TestAuthModule.ModuleB.name, TestSceneAuthenticatorWithAgg.permission!!.module)
        Assert.assertEquals(TestAuthType.Write.name, TestSceneAuthenticatorWithAgg.permission!!.type)
        Assert.assertEquals(PermissionState.Undefined, TestSceneAuthenticatorWithAgg.permission!!.state)
    }

    @Test
    fun testAuthPassWithGroupAuth() {
        val context = execute("one_group", TestAuthModule.ModuleB, TestAuthType.Write)

        Assert.assertEquals(InterceptorResult.Continue, context.interceptorResult)
        Assert.assertNull(context.exception)
        Assert.assertEquals(TestAuthModule.ModuleB.name, TestSceneAuthenticator.permission!!.module)
        Assert.assertEquals(TestAuthType.Write.name, TestSceneAuthenticator.permission!!.type)
        Assert.assertEquals(PermissionState.Allowed, TestSceneAuthenticator.permission!!.state)
    }

    @Test
    fun testAuthPassWithGroupAuthWithAgg() {
        val context = execute("one_group", TestAuthModule.ModuleB, TestAuthType.Write, isAgg = true)

        Assert.assertEquals(InterceptorResult.Continue, context.interceptorResult)
        Assert.assertNull(context.exception)
        Assert.assertEquals(TestAuthModule.ModuleB.name, TestSceneAuthenticatorWithAgg.permission!!.module)
        Assert.assertEquals(TestAuthType.Write.name, TestSceneAuthenticatorWithAgg.permission!!.type)
        Assert.assertEquals(PermissionState.Allowed, TestSceneAuthenticatorWithAgg.permission!!.state)
    }

    @Test
    fun testAuthNoPassWithGroupAuth() {
        val context = execute("one_group", TestAuthModule.ModuleB, TestAuthType.Read)

        Assert.assertEquals(InterceptorResult.Stop, context.interceptorResult)
        Assert.assertNotNull(context.exception)
        Assert.assertTrue(context.exception is NoPermissionException)
        Assert.assertEquals(TestAuthModule.ModuleB.name, TestSceneAuthenticator.permission!!.module)
        Assert.assertEquals(TestAuthType.Read.name, TestSceneAuthenticator.permission!!.type)
        Assert.assertEquals(PermissionState.Forbidden, TestSceneAuthenticator.permission!!.state)
    }

    @Test
    fun testAuthNoPassWithGroupAuthWithAgg() {
        val context = execute("one_group", TestAuthModule.ModuleB, TestAuthType.Read, isAgg = true)

        Assert.assertEquals(InterceptorResult.Stop, context.interceptorResult)
        Assert.assertNotNull(context.exception)
        Assert.assertTrue(context.exception is NoPermissionException)
        Assert.assertEquals(TestAuthModule.ModuleB.name, TestSceneAuthenticatorWithAgg.permission!!.module)
        Assert.assertEquals(TestAuthType.Read.name, TestSceneAuthenticatorWithAgg.permission!!.type)
        Assert.assertEquals(PermissionState.Forbidden, TestSceneAuthenticatorWithAgg.permission!!.state)
    }

    @Test
    fun testAuthPassWithDeeperGroupAuth() {
        val context = execute("two_higher_groups", TestAuthModule.ModuleC, TestAuthType.Write)

        Assert.assertEquals(InterceptorResult.Continue, context.interceptorResult)
        Assert.assertNull(context.exception)
        Assert.assertEquals(TestAuthModule.ModuleC.name, TestSceneAuthenticator.permission!!.module)
        Assert.assertEquals(TestAuthType.Write.name, TestSceneAuthenticator.permission!!.type)
        Assert.assertEquals(PermissionState.Allowed, TestSceneAuthenticator.permission!!.state)
    }

    @Test
    fun testAuthPassWithDeeperGroupAuthWithAgg() {
        val context = execute("two_higher_groups", TestAuthModule.ModuleC, TestAuthType.Write, isAgg = true)

        Assert.assertEquals(InterceptorResult.Continue, context.interceptorResult)
        Assert.assertNull(context.exception)
        Assert.assertEquals(TestAuthModule.ModuleC.name, TestSceneAuthenticatorWithAgg.permission!!.module)
        Assert.assertEquals(TestAuthType.Write.name, TestSceneAuthenticatorWithAgg.permission!!.type)
        Assert.assertEquals(PermissionState.Allowed, TestSceneAuthenticatorWithAgg.permission!!.state)
    }

    @Test
    fun testAuthPassWithBranchedGroupAuth() {
        val context = execute("two_branch_groups", TestAuthModule.ModuleB, TestAuthType.Write)

        Assert.assertEquals(InterceptorResult.Continue, context.interceptorResult)
        Assert.assertNull(context.exception)
        Assert.assertEquals(TestAuthModule.ModuleB.name, TestSceneAuthenticator.permission!!.module)
        Assert.assertEquals(TestAuthType.Write.name, TestSceneAuthenticator.permission!!.type)
        Assert.assertEquals(PermissionState.Allowed, TestSceneAuthenticator.permission!!.state)
    }

    @Test
    fun testAuthPassWithBranchedGroupAuthWithAgg() {
        val context = execute("two_branch_groups", TestAuthModule.ModuleB, TestAuthType.Write, isAgg = true)

        Assert.assertEquals(InterceptorResult.Continue, context.interceptorResult)
        Assert.assertNull(context.exception)
        Assert.assertEquals(TestAuthModule.ModuleB.name, TestSceneAuthenticatorWithAgg.permission!!.module)
        Assert.assertEquals(TestAuthType.Write.name, TestSceneAuthenticatorWithAgg.permission!!.type)
        Assert.assertEquals(PermissionState.Allowed, TestSceneAuthenticatorWithAgg.permission!!.state)
    }
}