package io.github.notsyncing.manifold.hooking

import java.util.concurrent.CompletableFuture

interface Hook<T> {
    fun execute(inputValue: T?): CompletableFuture<T?>
}