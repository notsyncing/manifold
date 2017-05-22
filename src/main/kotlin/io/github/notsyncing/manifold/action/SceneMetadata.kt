package io.github.notsyncing.manifold.action

import io.github.notsyncing.manifold.action.describe.DataPolicy

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SceneMetadata(val value: String, val dataPolicy: DataPolicy = DataPolicy.Get)