package io.github.notsyncing.manifold.feature

import io.github.notsyncing.manifold.authenticate.SpecialAuth

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Feature(val value: String, val groups: Array<String> = arrayOf(), val successorOf: String = "",
                         val internal: Boolean = false,
                         val defaultSpecialAuths: Array<SpecialAuth> = arrayOf())