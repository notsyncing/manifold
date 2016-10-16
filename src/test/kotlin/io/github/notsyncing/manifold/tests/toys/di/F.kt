package io.github.notsyncing.manifold.tests.toys.di

import io.github.notsyncing.manifold.di.EarlyProvide
import io.github.notsyncing.manifold.di.ProvideAsSingleton

@ProvideAsSingleton
@EarlyProvide
class F {
}