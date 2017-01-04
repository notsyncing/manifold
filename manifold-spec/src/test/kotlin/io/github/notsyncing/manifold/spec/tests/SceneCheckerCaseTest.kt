package io.github.notsyncing.manifold.spec.tests

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.feature.FeatureAuthenticator
import io.github.notsyncing.manifold.spec.tests.toys.Auth
import io.github.notsyncing.manifold.spec.tests.toys.Module
import io.github.notsyncing.manifold.spec.tests.toys.TestSpec
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutionException

class SceneCheckerCaseTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            FeatureAuthenticator.configure {
                our feature "ProductData" needs Module.ProductData type Auth.Edit
            }

            Manifold.features.enableFeatureGroups("TestGroup")

            Manifold.init()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            Manifold.reset()
        }
    }

    val spec = TestSpec()

    @Test
    fun testCheckSuccessFlow() {
        spec.checkCase("新增产品数据场景", "写入产品数据应当返回成功")
    }

    @Test
    fun testCheckFailedFlow() {
        spec.checkCase("新增产品数据场景", "未填写产品名称时应当返回失败")
    }

    @Test
    fun testCheckFlowWithWrongExitPoint() {
        try {
            spec.checkCase("新增产品数据场景", "测试错误结束点")
            assertTrue(false)
        } catch (e: ExecutionException) {
            assertTrue(e.cause?.message!!.contains("unexpected flow"))
        }
    }

    @Test
    fun testCheckFlowWithWrongExitResult() {
        try {
            spec.checkCase("新增产品数据场景", "测试错误返回值")
            assertTrue(false)
        } catch (e: ExecutionException) {
            assertTrue(e.cause?.message!!.contains("unexpected result"))
        }
    }

    @Test
    fun testCheckFlowWithFailedAdditionalCondition() {
        try {
            spec.checkCase("新增产品数据场景", "测试其他条件失败")
            assertTrue(false)
        } catch (e: ExecutionException) {
            assertTrue(e.cause?.message!!.contains("unmet condition"))
        }
    }

    @Test
    fun testCheckCaseWithDatabase() {
        spec.checkCase("测试场景5", "数据库测试用例")
    }

    @Test
    fun testCheckCaseWithDatabaseMultipleSQL() {
        spec.checkCase("测试场景5", "数据库多条SQL测试用例")
    }

    @Test
    fun testCheckCaseWithResultInto() {
        spec.checkCase("测试场景6", "测试存放返回结果")
    }

    @Test
    fun testCheckCaseWithResultIntoOnly() {
        spec.checkCase("测试场景6", "测试仅存放返回结果")
    }

    @Test
    fun testCheckCaseWithSessionData() {
        spec.checkCase("测试场景6", "测试存放会话存储")
    }

    @Test
    fun testCheckFlowWithBranch() {
        spec.checkCase("测试场景7", "测试返回A")
        spec.checkCase("测试场景7", "测试返回B")
    }

    @Test
    fun testCheckFlowWithProbe() {
        spec.checkCase("测试场景8", "测试探针值")
    }
}