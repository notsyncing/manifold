package io.github.notsyncing.manifold.rules

import java.util.concurrent.CompletableFuture

class RuleTask(val name: String,
               val future: CompletableFuture<Any?>) {
}