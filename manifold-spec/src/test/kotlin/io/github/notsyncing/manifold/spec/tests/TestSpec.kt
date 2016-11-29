package io.github.notsyncing.manifold.spec.tests

import io.github.notsyncing.manifold.spec.ManifoldSpecification
import io.github.notsyncing.manifold.spec.SpecSection

class TestSpec : ManifoldSpecification() {
    override fun spec() = specification {
        it has 基础数据模块()
        it has 订单模块()
        it has 报表统计模块()
    }
}

class 基础数据模块 : SpecSection() {
    override fun spec() = specification {
        it has 产品数据()
        it has 安装员数据()
        it has 合作商数据()
        it has 操作员管理()
    }
}

fun end(a: Any) {}
fun and(f: () -> Unit) {}

class 产品数据 : SpecSceneGroup() {
    fun 新增产品数据场景() {
        feature name "ProductData"
        feature description "新增一个产品数据到数据库中"
        feature comment "随便写点注释"

        permission needs _Module.ProductData type Auth.Edit

        parameters {
            "name" to "产品名称"
            "details" to "详细介绍" can null
        }

        returns OperationResult::class.java

        actions {
            "获取当前公司ID" alias "a"
            "新增产品数据" alias "b"
        }

        flow {
            start goto "a"
            on { parameters["name"] == null } goto end(OperationResult.Failed, "未填写产品名称")
            on { "a".result <= 0 } goto end(OperationResult.Failed, "获取当前公司ID失败")
            "a" goto "b"
            "b" goto end("b".result, "成功")
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
                    }
                }
            }

            "未填写产品名称应当返回失败" on {
                given {
                    null into "name"
                    null into "details"
                }

                should {
                    exit at "未填写产品名称"

                    and {
                        // 其他检查
                    }
                }
            }
        }
    }

    fun 编辑产品数据场景() {

    }

    fun 删除产品数据场景() {

    }
}