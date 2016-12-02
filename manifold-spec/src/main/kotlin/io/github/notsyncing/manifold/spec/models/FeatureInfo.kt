package io.github.notsyncing.manifold.spec.models

import io.github.notsyncing.manifold.authenticate.SpecialAuth

class FeatureInfo {
    var name: String? = null
    var groups: Array<String>? = null
    var defaultSpecialAuths: Array<SpecialAuth> = emptyArray()
    var description: String? = null
    var comment: String? = null
    var internal: Boolean? = null

    infix fun name(name: String): FeatureInfo {
        this.name = name
        return this
    }

    infix fun group(group: String): FeatureInfo {
        this.groups = arrayOf(group)
        return this
    }

    infix fun groups(groups: Array<String>): FeatureInfo {
        this.groups = groups
        return this
    }

    infix fun defaultSpecialAuth(auth: SpecialAuth): FeatureInfo {
        this.defaultSpecialAuths = arrayOf(auth)
        return this
    }

    infix fun defaultSpecialAuths(auths: Array<SpecialAuth>): FeatureInfo {
        this.defaultSpecialAuths = auths
        return this
    }

    infix fun description(description: String): FeatureInfo {
        this.description = description
        return this
    }

    infix fun comment(comment: String): FeatureInfo {
        this.comment = comment
        return this
    }

    infix fun internal(internal: Boolean): FeatureInfo {
        this.internal = internal
        return this
    }
}