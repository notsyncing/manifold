package io.github.notsyncing.manifold.spec.i18n

import io.github.notsyncing.manifold.spec.ManifoldSpecification
import io.github.notsyncing.manifold.spec.SpecBuilder
import io.github.notsyncing.manifold.spec.SpecSceneGroup
import io.github.notsyncing.manifold.spec.annotations.SceneDef
import io.github.notsyncing.manifold.spec.database.DatabaseAccessor
import io.github.notsyncing.manifold.spec.database.DatabaseResult
import io.github.notsyncing.manifold.spec.database.UseDatabase
import io.github.notsyncing.manifold.spec.database.database
import io.github.notsyncing.manifold.spec.flow.*
import io.github.notsyncing.manifold.spec.models.*
import io.github.notsyncing.manifold.spec.testcase.TestCaseBuilder
import io.github.notsyncing.manifold.spec.testcase.TestCaseInfo

typealias 功能定义 = ManifoldSpecification
typealias 场景组 = SpecSceneGroup

fun ManifoldSpecification.功能定义(spec: SpecBuilder.() -> Unit): SpecBuilder {
    return this.specification(spec)
}

typealias 场景定义 = SceneDef
typealias 使用数据库 = UseDatabase

val SpecSceneGroup.功能: FeatureInfo
    get() = this.feature

val SpecSceneGroup.权限: PermissionInfo
    get() = this.permission

val SpecSceneGroup.参数: ParameterListInfo
    get() = this.parameters

val SpecSceneGroup.返回: ReturnInfo
    get() = this.returns

val SpecSceneGroup.流程: FlowBuilder
    get() = this.flow

val SpecSceneGroup.测试用例: TestCaseBuilder
    get() = this.cases

infix fun FeatureInfo.名称(name: String): FeatureInfo {
    return this.name(name)
}

infix fun FeatureInfo.组(group: String): FeatureInfo {
    return this.group(group)
}

infix fun FeatureInfo.默认特殊权限(auth: String): FeatureInfo {
    return this.defaultSpecialAuth(auth)
}

infix fun FeatureInfo.说明(description: String): FeatureInfo {
    return this.description(description)
}

infix fun FeatureInfo.备注(comment: String): FeatureInfo {
    return this.comment(comment)
}

infix fun FeatureInfo.是否内部使用(internal: Boolean): FeatureInfo {
    return this.internal(internal)
}

const val 是 = true
const val 否 = false

infix fun PermissionInfo.模块(module: Enum<*>): PermissionInfo {
    return this.needs(module)
}

infix fun PermissionInfo.类型(type: Enum<*>): PermissionInfo {
    return this.type(type)
}

val 为空 = null
val 字符串 = String::class.java
val 整数 = Int::class.java

infix fun ParameterInfo.类型(t: Class<*>): ParameterInfo {
    return this.type(t)
}

infix fun ParameterInfo.可以(v: Any?): ParameterInfo {
    return this.can(v)
}

infix fun FlowBuilder.当(cond: String): FlowCondItem {
    return this.on(cond)
}

infix fun FlowBuilder.当(cond: FlowCheckCondItem.() -> Boolean): FlowCheckCondItem {
    return this.on(cond)
}

infix fun FlowBuilder.执行(action: String): FlowActionItem {
    return this.goto(action)
}

infix fun FlowBuilder.前往(item: FlowItem): FlowItem {
    return this.goto(item)
}

infix fun FlowBuilder.结束(exitName: String): FlowExitItem {
    return this.end(exitName)
}

fun FlowCheckCondItem.参数(name: String): Any? {
    return this.parameter(name)
}

fun TestCaseInfo.给定(conds: TestCaseInfo.() -> Unit): TestCaseInfo {
    return this.given(conds)
}

fun TestCaseInfo.其他(proc: () -> Unit): TestCaseInfo {
    return this.others(proc)
}

fun TestCaseInfo.应当(checks: TestCaseInfo.() -> Unit): TestCaseInfo {
    return this.should(checks)
}

fun TestCaseInfo.跳过(): TestCaseInfo {
    return this.skip()
}

val TestCaseInfo.结束: TestCaseInfo.TestCaseExitPoint
    get() = this.exit

infix fun TestCaseInfo.TestCaseExitPoint.于(exitName: String): TestCaseInfo.TestCaseExitPoint {
    return this.at(exitName)
}

infix fun TestCaseInfo.TestCaseExitPoint.并返回(result: Any?): TestCaseInfo.TestCaseExitPoint {
    return this.with(result)
}

infix fun TestCaseInfo.TestCaseExitPoint.并将结果存放于(variable: Ref<*>): TestCaseInfo.TestCaseExitPoint {
    return this.resultInto(variable)
}

fun TestCaseInfo.且满足(name: String, cond: () -> Boolean): TestCaseInfo {
    return this.and(name, cond)
}

val 数据库 = database

infix fun DatabaseAccessor.执行(sql: String): DatabaseResult {
    return this.execute(sql)
}

infix fun DatabaseAccessor.存在(sql: String): Boolean {
    return this.exists(sql)
}

infix fun DatabaseAccessor.不存在(sql: String): Boolean {
    return this.notExists(sql)
}

infix fun DatabaseResult.将(columnName: String): DatabaseResult.DatabaseResultStore {
    return this.store(columnName)
}

infix fun DatabaseResult.DatabaseResultStore.存入(variable: Ref<*>) {
    this.into(variable)
}