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

function DramaPropertyRepositoryFactory() {
    this.mixinMap = {};
}

function makeMixin(target, mixins) {
    for (var i = 0; i < mixins.length; i++) {
        for (var methodName in mixins[i]) {
            target[methodName] = mixins[i][methodName];
        }
    }

    return target;
}

DramaPropertyRepositoryFactory.get = function (javaClassName, domain) {
    var repoClass = DramaUtils.type(javaClassName, domain || __MANIFOLD_DRAMA_CURRENT_DOMAIN__);

    if (!repoClass) {
        return null;
    }

    repoClass = repoClass.static;

    var __this = this;

    return new JSAdapter() {
        __new__: function (context) {
            var repo = new repoClass(context);

            return new JSAdapter() {
                __get__: function (name) {
                    return repo[name];
                },
                __put__: function (name, value) {
                    repo[name] = value;
                },
                __call__: function () {
                    var methodName = arguments[0];
                    var args = [repo].concat(Array.prototype.slice.call(arguments, 1));
                    var result = Function.call.apply(repo[methodName], args);

                    if (methodName === "get") {
                        return new Promise(function (resolve, reject) {
                            result.thenAccept(function (r) {
                                var mixins = __this.mixinMap[r.class.name];

                                if (mixins) {
                                    makeMixin(r, mixins);
                                }

                                resolve(r);
                            }).exceptionally(reject);
                        });
                    }

                    return result;
                }
            };
        }
    };
};

DramaPropertyRepositoryFactory.mixin = function (javaClassName, classToMixin) {
    if (!this.mixinMap[javaClassName]) {
        this.mixinMap[javaClassName] = [];
    }

    this.mixinMap[javaClassName].push(classToMixin);
};

function repo(javaClassName, domain) {
    return DramaPropertyRepositoryFactory.get(javaClassName, domain || __MANIFOLD_DRAMA_CURRENT_DOMAIN__);
}

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