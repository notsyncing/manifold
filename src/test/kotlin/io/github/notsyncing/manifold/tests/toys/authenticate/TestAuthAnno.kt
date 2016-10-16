package io.github.notsyncing.manifold.tests.toys.authenticate

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestAuthAnno(val value: TestAuthModule, val type: TestAuthType)