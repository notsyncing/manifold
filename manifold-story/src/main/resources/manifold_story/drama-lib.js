"use strict";

var DramaManager = Java.type("io.github.notsyncing.manifold.story.drama.DramaManager");
var CompletableFuture = Java.type("java.util.concurrent.CompletableFuture");
var DramaUtils = Java.type("io.github.notsyncing.manifold.story.drama.DramaUtils");
var Manifold = Java.type("io.github.notsyncing.manifold.Manifold");

function Promise(handlerOrCf) {
    if (typeof handlerOrCf === "function") {
        this._cf = new CompletableFuture();
        var __this = this;

        handlerOrCf.call(this, function (r) {
            __this._cf.complete(r);
        }, function (err) {
            __this._cf.completeExceptionally(err);
        });
    } else if (handlerOrCf instanceof CompletableFuture) {
        this._cf = handlerOrCf;
    } else {
        throw new Error("Unsupported parameter of promise: " + handlerOrCf);
    }
}

Promise.prototype.then = function (onFulfilled, onRejected) {
    var f = this._cf.thenCompose(function (r) {
        var result = onFulfilled(r);

        if (result instanceof Promise) {
            return result.toCompletableFuture();
        } else {
            return CompletableFuture.completedFuture(result);
        }
    });

    if (onRejected) {
        f = f.exceptionally(onRejected);
    }

    return f;
};

Promise.prototype.catch = function (onRejected) {
    return this._cf.exceptionally(onRejected);
};

Promise.prototype.toCompletableFuture = function () {
    return this._cf;
};

Promise.resolve = function (r) {
    return new Promise(function (resolve, reject) {
        resolve(r);
    });
};

Promise.reject = function (r) {
    return new Promise(function (resolve, reject) {
        reject(r);
    });
};

function setCurrentDomain(domain) {
    __MANIFOLD_DRAMA_CURRENT_DOMAIN__ = domain;
}

function toPromise(cf) {
    return new Promise(cf);
}

function toCompletableFuture(promise) {
    return promise.toCompletableFuture();
}

function Role(permissionName, permissionType) {
    this.permissionName = permissionName || null;
    this.permissionType = permissionType || null;
}

Role.prototype.on = function (actionName, handler) {
    var h = function (context, parameters, permissionParameters) {
        parameters = JSON.parse(parameters);

        if (permissionParameters) {
            permissionParameters = JSON.parse(permissionParameters);
        }

        var result = handler(context, parameters, permissionParameters);

        if (result instanceof Promise) {
            return toCompletableFuture(result);
        } else if (!(result instanceof CompletableFuture)) {
            return CompletableFuture.completedFuture(result);
        } else {
            return result;
        }
    };

    Object.freeze(h);
    Object.seal(h);

    DramaManager.registerAction(actionName, this.permissionName, this.permissionType, h,
        __MANIFOLD_DRAMA_CURRENT_FILE__, __MANIFOLD_DRAMA_CURRENT_DOMAIN__);
};

function type(javaClassName, domain) {
    var _domain = domain || __MANIFOLD_DRAMA_CURRENT_DOMAIN__;
    return DramaUtils.type(javaClassName, _domain).static;
}

function from(type, json) {
    if (typeof json === "object") {
        json = JSON.stringify(json);
    }

    return DramaUtils.jsonToObject(type.class, json);
}

function hooking(name, handler) {
    var domain = __MANIFOLD_DRAMA_CURRENT_DOMAIN__;
    var hooker = handler(Java.extend(Java.type("io.github.notsyncing.manifold.hooking.Hook")));
    var hookSource = __MANIFOLD_DRAMA_CURRENT_FILE__ + "?" + name;

    if (domain) {
        hookSource += "@" + domain;
    }

    Manifold.hooks.registerHook(name, domain, hooker, hookSource);
}