package io.github.notsyncing.manifold.rules

interface Rule {
    var name: String
    var content: String
    var parameters: List<RuleParameter>
}