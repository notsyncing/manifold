"use strict";

var DramaManager = Java.type("io.github.notsyncing.manifold.story.drama.DramaManager");
var CompletableFuture = Java.type("java.util.concurrent.CompletableFuture");
var DramaUtils = Java.type("io.github.notsyncing.manifold.story.drama.utils");

function Role(permissionName, permissionType) {
    this.permissionName = permissionName;
    this.permissionType = permissionType;
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
    var t = DramaUtils.type(javaClassName, _domain);

    t.from = function (objOrJson) {
        var json = objOrJson;

        if (typeof json === "object") {
            json = JSON.stringify(json);
        }

        return DramaUtils.jsonToObject(javaClassName, json, _domain);
    };

    return t;
}