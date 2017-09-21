"use strict";

(function () {
    var user = new Role();

    user.on("simpleAction", function (context, params) {
        return toCompletableFuture(Promise.resolve("Hello, world!"));
    });

    var authUser = new Role("TestModule", "TestAuth");

    authUser.on("simpleAuthAction", function (context, params) {
        return toCompletableFuture(Promise.resolve("Hello, user!"));
    });
})();
