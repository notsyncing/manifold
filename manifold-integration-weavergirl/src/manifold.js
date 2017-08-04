class Manifold {
    static sceneUrl(method, name, parameters, sessionIdentifier, namespace) {
        let url = `${Manifold.serverUrl}/${name}`;
        let payload = "";

        if (parameters) {
            if (parameters instanceof FormData) {
                payload = parameters;
            } else {
                payload = `json=${encodeURIComponent(JSON.stringify(parameters))}`;
            }
        }

        if (sessionIdentifier) {
            if (payload instanceof FormData) {
                payload.append("access_token", sessionIdentifier);
            } else {
                if (payload !== "") {
                    payload += "&";
                }

                payload += `access_token=${sessionIdentifier}`;
            }
        }

        namespace = namespace || Manifold.namespace;

        if (namespace) {
            if (payload instanceof FormData) {
                payload.append("namespace", namespace);
            } else {
                if (payload !== "") {
                    payload += "&";
                }

                payload += `namespace=${namespace}`;
            }
        }

        if (payload) {
            if (method === "get") {
                url += `?${payload}`;
                payload = null;
            }
        }

        return {
            url: url,
            payload: payload
        };
    }

    static getSceneUrl(method, name, parameters, sessionIdentifier, namespace) {
        let o = Manifold.sceneUrl(method, name, parameters, sessionIdentifier, namespace);
        return o.url;
    }

    static callScene(method, name, parameters, sessionIdentifier, namespace) {
        let o = Manifold.sceneUrl(method, name, parameters, sessionIdentifier, namespace);

        return Weavergirl.Http.ajax(method, o.url, o.payload)
            .then(r => r.response);
    }

    static _serializeForm(form) {
        let json = Weavergirl.Form.serializeToJson(form);

        if (form.enctype === "multipart/form-data") {
            let formData = new FormData();
            formData.append("json", JSON.stringify(json));

            for (let e of form.childNodes) {
                if (!(e instanceof HTMLInputElement)) {
                    continue;
                }

                if (e.type !== "file") {
                    continue;
                }

                for (let f of e.files) {
                    formData.append(e.name, f);
                }
            }

            return formData;
        }

        return json;
    }

    static _mergeObjects(objs) {
        if (!(objs instanceof Array)) {
            return objs;
        }

        let base = null;

        for (let o of objs) {
            if (o instanceof HTMLFormElement) {
                o = Manifold._serializeForm(o);
            }

            if (!base) {
                base = o;
                continue;
            }

            for (let k of Object.keys(o)) {
                base[k] = o[k];
            }
        }

        return base;
    }

    static getScene(name, parameters, sessionIdentifier, namespace) {
        if (parameters instanceof Array) {
            parameters = Manifold._mergeObjects(parameters);
        }

        if (parameters instanceof Element) {
            parameters = Manifold._serializeForm(parameters);
        }

        return Manifold.callScene("get", name, parameters, sessionIdentifier, namespace);
    }

    static postScene(name, parameters, sessionIdentifier, namespace) {
        if (parameters instanceof Array) {
            parameters = Manifold._mergeObjects(parameters);
        }

        if (parameters instanceof Element) {
            parameters = Manifold._serializeForm(parameters);
        }

        return Manifold.callScene("post", name, parameters, sessionIdentifier, namespace);
    }

    static performDramaAction(name, parameters, sessionIdentifier, namespace) {
        if (parameters instanceof Array) {
            parameters = Manifold._mergeObjects(parameters);
        }

        if (parameters instanceof Element) {
            parameters = Manifold._serializeForm(parameters);
        }

        let p = {
            action: name,
            parameters: parameters
        };

        return postScene("manifold.drama.entry", p, sessionIdentifier, namespace);
    }
}

Manifold.serverUrl = "http://localhost:8080/manifold/gateway";
Manifold.namespace = null;

try {
    if (module) {
        module.exports = Manifold;
    }
} catch (err) {

}