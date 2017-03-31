package io.github.notsyncing.manifold.action

@Deprecated("Renamed to ManifoldTransactionalAction", replaceWith = ReplaceWith("ManifoldTransactionalAction<T, R>"))
abstract class ManifoldDatabaseAction<T, R>(transClass: Class<T>) : ManifoldTransactionalAction<T, R>(transClass)