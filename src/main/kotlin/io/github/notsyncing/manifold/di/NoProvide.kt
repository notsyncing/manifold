package io.github.notsyncing.manifold.di

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class NoProvide
