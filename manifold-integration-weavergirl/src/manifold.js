class Manifold {
    static sceneUrl(method, name, parameters, sessionIdentifier) {
        let url = `${Manifold.serverUrl}/${name}`;
        let payload = "";

        if (parameters) {
            payload = `json=${encodeURIComponent(JSON.stringify(parameters))}`;
        }

        if (sessionIdentifier) {
            if (payload !== "") {
                payload += "&";
            }

            payload += `access_token=${sessionIdentifier}`;
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

    static getSceneUrl(method, name, parameters, sessionIdentifier) {
        let o = Manifold.sceneUrl(method, name, parameters, sessionIdentifier);
        return o.url;
    }

    static callScene(method, name, parameters, sessionIdentifier) {
        let o = Manifold.sceneUrl(method, name, parameters, sessionIdentifier);

        return Weavergirl.Http.ajax(method, o.url, o.payload)
            .then(r => r.response);
    }

    static _serializeForm(form) {
        if (form.enctype === "multipart/form-data") {
            // FIXME: Implement this!
        } else {
            return Weavergirl.Form.serializeToJson(form);
        }
    }

    static getScene(name, parameters, sessionIdentifier) {
        if (parameters instanceof Element) {
            parameters = Manifold._serializeForm(parameters);
        }

        return Manifold.callScene("get", name, parameters, sessionIdentifier);
    }

    static postScene(name, parameters, sessionIdentifier) {
        if (parameters instanceof Element) {
            parameters = Manifold._serializeForm(parameters);
        }

        return Manifold.callScene("post", name, parameters, sessionIdentifier);
    }
}

Manifold.serverUrl = "http://localhost:8080/service/gateway";

try {
    if (module) {
        module.exports = Manifold;
    }
} catch (err) {

}