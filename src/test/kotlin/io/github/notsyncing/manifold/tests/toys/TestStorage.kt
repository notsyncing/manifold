package io.github.notsyncing.manifold.tests.toys

import io.github.notsyncing.manifold.storage.ManifoldStorage

class TestStorage : ManifoldStorage<String>() {
    companion object {
        lateinit var result: String

        fun reset() {
            result = ""
        }
    }

    fun test() {
        result = db
    }
}