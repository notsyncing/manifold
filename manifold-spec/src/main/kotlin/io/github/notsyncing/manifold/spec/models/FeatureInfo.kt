package io.github.notsyncing.manifold.spec.models

class FeatureInfo {
    var name: String = ""
    var description: String = ""
    var comment: String = ""

    infix fun name(name: String) {
        this.name = name
    }

    infix fun description(description: String) {
        this.description = description
    }

    infix fun comment(comment: String) {
        this.comment = comment
    }
}