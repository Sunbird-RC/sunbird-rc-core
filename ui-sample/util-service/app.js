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
const request = require('request')
const _ = require('lodash')
var async = require('async');


const port = process.env.PORT || 8090;

app.use(cors())
app.use(morgan('dev'));
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

app.post("/register/users", async (req, res) => {
    createUser(req.body, req.headers, function (err, data) {
        if (err) {
            // res.statusCode = err.statusCode;
            return res.send(err.body)
        } else {
            return res.send(data);
        }
    });
});

const createUser = (value, header, callback) => {
    async.waterfall([
        function (callback) {
            addUserToKeycloak(value.request, header, callback);
        },
        function (value, header, res, callback2) {
            console.log("Employee successfully added to registry")
            addEmployeeToRegistry(value, header, res, callback2)
        }
    ], function (err, result) {
        console.log('Main Callback --> ' + result);
        if (err) {
            callback(err, null)
        } else
            callback(null, result)
    });
}


app.post("/registry/add", async (req, res, next) => {
    postCallToRegistry(req.body, "/add", function (err, data) {
        return res.send(data);
    });
});

app.post("/registry/search", async (req, res, next) => {
    postCallToRegistry(req.body, "/search", function (err, data) {
        return res.send(data);
    });
});

app.post("/registry/read", async (req, res, next) => {
    postCallToRegistry(req.body, "/read", function (err, data) {
        return res.send(data);
    })
});

app.post("/registry/update", async (req, res, next) => {
    postCallToRegistry(req.body, "/update", function (err, data) {
        return res.send(data);
    })
});

const postCallToRegistry = (value, endPoint, callback) => {
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
    request(options, function (err, res) {
        if (res.body.responseCode === 'OK') {
            callback(null, res.body)
        }
        else {
            callback(new Error(_.get(res, 'params.errmsg') || _.get(res, 'params.err')), res.body);
        }
    });
}

const addEmployeeToRegistry = (value, header, res, callback) => {
    value['isApproved'] = false;
    let reqBody = {
        "id": "open-saber.registry.create",
        "ver": "1.0",
        "ets": "11234",
        "params": {
            "did": "",
            "key": "",
            "msgid": ""
        },
        "request": {
            "Employee": value
        }
    }
    const options = {
        method: 'POST',
        url: registryHost + '/add',
        json: true,
        headers: {
            'content-type': 'application/json',
            'accept': 'application/json'
        },
        body: reqBody,
        json: true
    }
    if (res.statusCode == 201) {
        request(options, function (err, res, body) {
            if (res.statusCode == 200) {
                console.log("Employee successfully added to registry")
                callback(null, res.body)
            } else {
                console.log("Employee could not be added to registry" + res.statusCode)
                callback(res.statusCode, body.errorMessage)
            }
        })
    } else {
        callback(res, null)
    }
}

const addUserToKeycloak = (value, headers, callback) => {
    const options = {
        method: 'POST',
        url: keyCloakHost,
        json: true,
        headers: {
            'content-type': 'application/json',
            'accept': 'application/json',
            'Authorization': headers.authorization
        },
        body: {
            username: value.email,
            enabled: true,
            emailVerified: false,
            firstName: value.name,
            email: value.email,
            requiredActions: [
                "UPDATE_PASSWORD"
            ],
            credentials: [
                {
                    "value": "password",
                    "type": "password"
                }
            ]
        },
        json: true
    }
    request(options, function (err, res, body) {
        callback(null, value, headers, res)
    })
}



startServer = () => {
    server.listen(port, function () {
        console.log("util service listening on port " + port);
    })
};

startServer();