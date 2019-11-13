const express = require("express");
const http = require("http");
const app = express();
var bodyParser = require("body-parser");
var cors = require("cors")
const morgan = require("morgan");
const server = http.createServer(app);
const registryHost = process.env.registryHost || "http://localhost:8081";
const realmName = process.env.realmName || "PartnerRegistry"
const keyCloakHost = process.env.keyCloakHost || "http://localhost:8080/auth/admin/realms/" + realmName + "/users";
const request = require('request-promise')
const _ = require('lodash')
var async = require('async');


const port = process.env.PORT || 8090;

app.use(cors())
app.use(morgan('dev'));
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());


app.post("/register/users", async (req, res, next) => {
    try {
        let response = await registerUser(req.body, req.headers);
        return res.send(response)
    } catch (error) {
        res.status(error.statusCode)
        return res.send(error.error);
    }

});

app.post("/registry/add", async (req, res, next) => {
    return res.send(await postCallToRegistry(req.body, "/add"));
});

app.post("/registry/search", async (req, res, next) => {
    return res.send(await postCallToRegistry(req.body, "/search"));
});

app.post("/registry/read", async (req, res, next) => {
    return res.send(await postCallToRegistry(req.body, "/read"));
});

app.post("/registry/update", async (req, res, next) => {
    return res.send(await postCallToRegistry(req.body, "/update"));
});

app.post("/formTemplate", (req, res, next) => {
    return res.send(await getFormTemplates(req.body));
});

const getFormTemplates = (value) => {

}
const postCallToRegistry = (value, endPoint) => {
    const options = {
        method: 'POST',
        url: registryHost + endPoint,
        json: true,
        headers: {
            'content-type': 'application/json',
            'accept': 'application/json'
        },
        body: value,
        json: true
    }
    return request(options).then(data => {
        if (data.responseCode === 'OK') {
            return data;
        } else {
            throw new Error(_.get(data, 'params.errmsg') || _.get(data, 'params.err'));
        }
    })
}

const registerUser = (value, headers) => {
    const options = {
        method: 'POST',
        url: keyCloakHost,
        json: true,
        headers: {
            'content-type': 'application/json',
            'accept': 'application/json',
            'Authorization': headers.authorization
        },
        body: value,
        json: true
    }
    return request(options).then(data => {
        if (data) {
            return data;
        }
    });
}



startServer = () => {
    server.listen(port, function () {
        console.log("util service listening on port " + port);
    })
};

startServer();