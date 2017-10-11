"use strict";

var DramaManager = Java.type("io.github.notsyncing.manifold.story.drama.DramaManager");
var CompletableFuture = Java.type("java.util.concurrent.CompletableFuture");
var DramaUtils = Java.type("io.github.notsyncing.manifold.story.drama.DramaUtils");
var Manifold = Java.type("io.github.notsyncing.manifold.Manifold");

function setCurrentDomain(domain) {
    __MANIFOLD_DRAMA_CURRENT_DOMAIN__ = domain;
}

function toPromise(cf) {
    return new Promise(function (resolve, reject) {
        cf.thenAccept(resolve).exceptionally(reject);
    });
}

function toCompletableFuture(promise) {
    var cf = new CompletableFuture();

    promise.then(function (r) {
        cf.complete(r);
    }).catch(function (err) {
        cf.completeExceptionally(err);
    });

    return cf;
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
            var f = new CompletableFuture();

            result.then(f.complete)
                .catch(f.completeExceptionally);

            return f;
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

function fill(obj, json) {
    if (typeof json === "object") {
        json = JSON.stringify(json);
    }

    if (!obj.class) {
        throw new Error("You cannot fill a non-Java object " + obj);
    }

    return DramaUtils.jsonToObject(obj.class, json);
}

function hooking(name, domain, handler) {
    if (typeof domain === "function") {
        handler = domain;
        domain = null;
    }

    var hooker = handler(Java.extend(Java.type("io.github.notsyncing.manifold.hooking.Hook")));

    Manifold.hooks.registerHook(name, domain, hooker);
}