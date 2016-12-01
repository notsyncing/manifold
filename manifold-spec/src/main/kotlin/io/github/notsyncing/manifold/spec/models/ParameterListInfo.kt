package io.github.notsyncing.manifold.spec.models

class ParameterListInfo(val parameters: MutableList<ParameterInfo> = mutableListOf()) {
    operator fun invoke(parameters: ParameterListInfo.() -> Unit) {
        this.parameters()
    }

    infix fun String.to(name: String): ParameterInfo {
        val p = ParameterInfo(name, this)
        parameters.add(p)
        return p
    }
}