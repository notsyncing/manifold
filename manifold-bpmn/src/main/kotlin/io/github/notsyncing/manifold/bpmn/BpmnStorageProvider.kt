package io.github.notsyncing.manifold.bpmn

import java.util.concurrent.CompletableFuture

interface BpmnStorageProvider {
    fun getDiagram(name: String): CompletableFuture<String>
    fun saveDiagram(name: String, data: String): CompletableFuture<Unit>
}