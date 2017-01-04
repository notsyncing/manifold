package io.github.notsyncing.manifold.spec.tests.toys

import io.github.notsyncing.manifold.authenticate.SpecialAuth
import io.github.notsyncing.manifold.spec.ManifoldSpecification
import io.github.notsyncing.manifold.spec.i18n.*
import io.github.notsyncing.manifold.spec.models.Ref

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

class TestSpec : ManifoldSpecification(useDatabase = true) {
    override fun spec() = 功能定义 {
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
            当("返回失败") then {
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
                    null into sessionIdentifier
                }

                应当 {
                    结束 于 "成功" 并返回 OperationResult.Success

                    且满足("其他检查") {
                        true
                    }
                }
            }

            "未填写产品名称时应当返回失败" {
                给定 {
                    "" into "name"
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

            "测试错误结束点" {
                给定 {
                    "测试1" into "name"
                    "这是测试1" into "details"
                    null into sessionIdentifier
                }

                应当 {
                    结束 于 "未填写产品名称" 并返回 OperationResult.Success
                }
            }

            "测试错误返回值" {
                给定 {
                    "测试1" into "name"
                    "这是测试1" into "details"
                    null into sessionIdentifier
                }

                应当 {
                    结束 于 "成功" 并返回 OperationResult.Failed
                }
            }

            "测试其他条件失败" {
                给定 {
                    "测试1" into "name"
                    "这是测试1" into "details"
                    null into sessionIdentifier
                }

                应当 {
                    结束 于 "成功" 并返回 OperationResult.Success

                    且满足("失败") {
                        false
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
            "nullableParam" to "测试可空" 类型 整数 可以 为空
        }
    }

    @场景定义
    fun 测试场景3A() {
        功能 名称 "TestScene3A"
        功能 组 "TestGroup"
        功能 说明 "测试用的场景3A"
        功能 备注 "随便"
        功能 是否内部使用 否

        参数 {
            "id" to "ID" 类型 整数
            "nullableParam" to "测试可空" 类型 整数 可以 为空
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

    @场景定义
    @使用数据库
    fun 测试场景5() {
        功能 名称 "TestScene5"
        功能 组 "TestGroup"
        功能 说明 "测试用的场景5"
        功能 备注 "随便"
        功能 是否内部使用 否

        返回(字符串)

        流程 {
            前往(结束("成功"))
        }

        测试用例 {
            "数据库测试用例" {
                给定 {
                    其他 {
                        val insertedId = Ref(0)
                        数据库 执行 "INSERT INTO test_table (name, value) VALUES ('a', 2) RETURNING id" 将 "id" 存入 insertedId
                    }
                }

                应当 {
                    结束 于 "成功" 并返回 "Success"

                    且满足("数据库中应有数据") {
                        数据库 存在 "SELECT 1 FROM test_table WHERE value = 2"
                    }
                }
            }

            "数据库多条SQL测试用例" {
                给定 {
                    其他 {
                        val insertedId = Ref(0)
                        数据库 执行 "INSERT INTO test_table (name, value) VALUES ('a', 2) RETURNING id" 将 "id" 存入 insertedId
                        数据库 执行 "INSERT INTO test_table (name, value) VALUES ('a', 3) RETURNING id" 将 "id" 存入 insertedId
                        数据库 执行 "INSERT INTO test_table (name, value) VALUES ('a', 4) RETURNING id" 将 "id" 存入 insertedId
                        数据库 执行 "INSERT INTO test_table (name, value) VALUES ('a', 5) RETURNING id" 将 "id" 存入 insertedId
                        数据库 执行 "INSERT INTO test_table (name, value) VALUES ('a', 6) RETURNING id" 将 "id" 存入 insertedId
                        数据库 执行 "INSERT INTO test_table (name, value) VALUES ('a', 7) RETURNING id" 将 "id" 存入 insertedId
                    }
                }

                应当 {
                    结束 于 "成功" 并返回 "Success"

                    且满足("数据库中应有数据") {
                        数据库 存在 "SELECT 1 FROM test_table WHERE value = 2"
                    }
                }
            }
        }
    }

    @场景定义
    fun 测试场景6() {
        功能 名称 "TestScene6"
        功能 组 "TestGroup"
        功能 说明 "测试用的场景6"
        功能 备注 "随便"
        功能 是否内部使用 否

        返回(字符串)

        流程 {
            前往(结束("成功"))
        }

        测试用例 {
            "测试存放返回结果" {
                val result = Ref<Any?>("")

                给定 {
                }

                应当 {
                    结束 于 "成功" 并返回 "Success" 并将结果存放于 result

                    且满足("存放结果正确") {
                        result.value == "Success"
                    }
                }
            }

            "测试仅存放返回结果" {
                val result = Ref<Any?>("")

                给定 {
                }

                应当 {
                    结束 于 "成功" 并将结果存放于 result

                    且满足("存放结果正确") {
                        result.value == "Success"
                    }
                }
            }

            "测试存放会话存储" {
                给定 {
                    "a" into sessionIdentifier
                    "test" into session("SESSION_KEY")
                }

                应当 {
                    结束 于 "成功"

                    且满足("会话存储中数据正确") {
                        session.get<String>("a", "SESSION_KEY") == "test"
                    }
                }
            }
        }
    }

    @场景定义
    fun 测试场景7() {
        功能 名称 "TestScene7"
        功能 组 "TestGroup"
        功能 说明 "测试用的场景7"
        功能 备注 "随便"
        功能 是否内部使用 否

        参数 {
            "cond" to "条件" 类型 Boolean::class.java
        }

        返回(字符串)

        流程 {
            当 { 参数("cond") == true } then 执行("A") or 执行("B")
            执行("C")
            前往(结束("成功"))
        }

        测试用例 {
            "测试返回A" {
                给定 {
                    true into "cond"
                }

                应当 {
                    结束 于 "成功" 并返回 "A"
                }
            }

            "测试返回B" {
                给定 {
                    false into "cond"
                }

                应当 {
                    结束 于 "成功" 并返回 "B"
                }
            }
        }
    }
}
