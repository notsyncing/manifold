package io.github.notsyncing.manifold.spec.tests

import io.github.notsyncing.manifold.spec.annotations.SceneDef

enum class Module {
    ProductData
}

enum class Auth {
    Edit
}

enum class OperationResult {
    Success,
    Failed
}

class TestSpec : 功能定义() {
    override fun spec() = specification {
        "基础数据模块" has {
            +产品数据()
            +安装员数据()
            +合作商数据()
            +操作员管理()
        }
        "订单模块" has {

        }
        "报表统计模块" has {

        }
    }
}

class 产品数据 : 场景组() {
    @SceneDef
    fun 新增产品数据场景() {
        feature name "ProductData"
        feature description "新增一个产品数据到数据库中"
        feature comment "随便写点注释"

        permission needs Module.ProductData type Auth.Edit

        parameters {
            "name" to "产品名称"
            "details" to "详细介绍" can null
        }

        returns(OperationResult::class.java)

        flow {
            on("产品名称为空") then end(OperationResult.Failed, "未填写产品名称")
            goto("获取当前公司ID")
            on("返回值 <= 0") then end(OperationResult.Failed, "获取当前公司ID失败")
            goto("新增产品数据")
            on("失败") then {
                goto(end(OperationResult.Failed, "新增产品失败"))
            } or {
                goto(end(OperationResult.Success, "成功"))
            }
        }

        cases {
            "写入产品数据应当返回成功" on {
                given {
                    "测试产品" into "name"
                    "这是用于测试的产品" into "details"
                }

                should {
                    exit at "成功"

                    and {
                        // 检查数据库里的数据
                        true
                    }
                }
            }

            "未填写产品名称时应当返回失败" on {
                given {
                    null into "name"
                    null into "details"
                }

                should {
                    exit at "未填写产品名称"

                    and {
                        // 其他检查
                        true
                    }
                }
            }
        }
    }

    @SceneDef
    fun 编辑产品数据场景() {

    }

    @SceneDef
    fun 删除产品数据场景() {

    }
}