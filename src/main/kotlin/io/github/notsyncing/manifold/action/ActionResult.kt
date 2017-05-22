package io.github.notsyncing.manifold.action

class ActionResult<R>(val succeed: Boolean, val errorCode: Int, val result: R?) {
    val failed = !succeed

    constructor(succeed: Boolean, errorCode: Enum<*>, result: R) : this(succeed, errorCode.ordinal, result)

    constructor(result: R) : this(true, 0, result)

    constructor(errorCode: Int) : this(false, errorCode, null)

    constructor(errorCode: Enum<*>) : this(errorCode.ordinal)
}