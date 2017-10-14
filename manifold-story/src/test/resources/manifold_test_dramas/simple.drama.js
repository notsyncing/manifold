"use strict";

(function () {
    var TestException = Java.type("io.github.notsyncing.manifold.story.tests.toys.TestException");

    var user = new Role();

    user.on("simpleAction", function (context, params) {
        return toCompletableFuture(Promise.resolve("Hello, world!"));
    });

    user.on("exceptionInJsAction", function (context, params) {
        throw new Error("EXCEPTION");
    });

    user.on("exceptionInJavaAction", function (context, params) {
        return TestException.exception();
    });

    var authUser = new Role("TestModule", "TestAuth");

    authUser.on("simpleAuthAction", function (context, params) {
        return toCompletableFuture(Promise.resolve("Hello, user!"));
    });
})();
