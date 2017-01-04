package io.github.notsyncing.manifold.spec.flow

class FlowCheckCondItem(val checkCond: FlowCheckCondItem.() -> Boolean) : FlowCondItem("") {
    private var currentProbes: Map<String, Any?>? = null

    fun parameter(name: String): Any? {
        return currentProbes!![name]
    }

    fun check(probes: Map<String, Any?>): Boolean {
        currentProbes = probes
        return this.checkCond()
    }
}