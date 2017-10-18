package io.github.notsyncing.manifold.rules

import java.util.concurrent.CompletableFuture

interface RuleStorageProvider {
    fun get(name: String): CompletableFuture<Rule?>

    fun getNameStartsWith(namePrefix: String): CompletableFuture<List<Rule>>

    fun getAll(): CompletableFuture<List<Rule>>

    fun put(name: String, content: String): CompletableFuture<Unit>

    fun put(model: Rule): CompletableFuture<Unit>

    fun delete(name: String): CompletableFuture<Unit>
}