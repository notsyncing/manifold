package io.github.notsyncing.manifold.tests.toys.features

import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.feature.Feature
import java.util.concurrent.CompletableFuture

@Feature(TestFeatures.Feature4, groups = arrayOf(TestFeatures.FeatureGroup2, TestFeatures.FeatureUpperGroup1))
class TestFeatureScene4 : ManifoldScene<String>(enableEventNode = false) {
    companion object {
        var initExecuted = false

        fun reset() {
            initExecuted = false
        }
    }

    override fun init() {
        super.init()

        initExecuted = true
    }

    override fun stage(): CompletableFuture<String> {
        return CompletableFuture.completedFuture("Feature4")
    }
}