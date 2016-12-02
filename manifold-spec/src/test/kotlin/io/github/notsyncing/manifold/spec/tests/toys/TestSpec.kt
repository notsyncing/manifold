package io.github.notsyncing.manifold.spec.tests.toys

import io.github.notsyncing.manifold.authenticate.SpecialAuth
import io.github.notsyncing.manifold.spec.i18n.*

enum class Module {
    ProductData
}

enum class Auth {
    View,
    Edit
}

enum class OperationResult {
    Success,
    Failed
}

class TestSpec : 功能定义() {
    override fun spec() = specification {
        "基础数据模块" {
            +产品数据()
        }
    }
}

class 产品数据 : 场景组() {
    @场景定义
    fun 新增产品数据场景() {
        功能 名称 "ProductData"
        功能 组 "TestGroup"
        功能 默认特殊权限 SpecialAuth.LoginOnly
        功能 说明 "新增一个产品数据到数据库中"
        功能 备注 "随便写点注释"
        功能 是否内部使用 否

        权限 模块 Module.ProductData 类型 Auth.Edit

        参数 {
            "name" to "产品名称" 类型 字符串
            "details" to "详细介绍" 类型 字符串 可以 为空
        }

        返回(OperationResult::class.java)

        流程 {
            当("产品名称为空") then 结束("未填写产品名称")
            执行("获取当前公司ID")
            当("返回值 <= 0") then 结束("获取当前公司ID失败")
            执行("新增产品数据")
            当("失败") then {
                前往(结束("新增产品失败"))
            } or {
                前往(结束("成功"))
            }
        }

        测试用例 {
            "写入产品数据应当返回成功" {
                给定 {
                    "测试产品" into "name"
                    "这是用于测试的产品" into "details"
                    "" into sessionIdentifier
                }

                应当 {
                    结束 于 "成功" 并返回 OperationResult.Success

                    且满足("数据库中应有数据") {
                        // 检查数据库里的数据
                        true
                    }

                    且满足("其他检查") {
                        true
                    }
                }
            }

            "未填写产品名称时应当返回失败" {
                给定 {
                    null into "name"
                    null into "details"
                    null into sessionIdentifier
                }

                应当 {
                    结束 于 "未填写产品名称" 并返回 OperationResult.Failed

                    且满足("其他检查") {
                        // 其他检查
                        true
                    }
                }
            }
        }
    }

    @场景定义
    fun 测试场景1() {
        功能 名称 "TestScene1"
        功能 组 "TestGroup"
        功能 说明 "测试用的场景1"
        功能 备注 "随便"
        功能 是否内部使用 否
    }

    @场景定义
    fun 测试场景2() {
        功能 名称 "TestScene2"
        功能 组 "TestGroup"
        功能 说明 "测试用的场景2"
        功能 备注 "随便"
        功能 是否内部使用 否

        权限 模块 Module.ProductData 类型 Auth.View
    }

    @场景定义
    fun 测试场景3() {
        功能 名称 "TestScene3"
        功能 组 "TestGroup"
        功能 说明 "测试用的场景3"
        功能 备注 "随便"
        功能 是否内部使用 否

        参数 {
            "uid" to "ID" 类型 整数
        }
    }

    @场景定义
    fun 测试场景4() {
        功能 名称 "TestScene4"
        功能 组 "TestGroup"
        功能 说明 "测试用的场景4"
        功能 备注 "随便"
        功能 是否内部使用 否

        返回(字符串)
    }
}
