const express = require("express");
const http = require("http");
const app = express();
var bodyParser = require("body-parser");
var cors = require("cors")
const morgan = require("morgan");
const server = http.createServer(app);
const registryHost = process.env.registry_url || "http://localhost:8081";
const realmName = process.env.keycloak_realmName || "PartnerRegistry"
const keyCloakHost = process.env.keyCloak_url || "http://localhost:8080/auth/admin/realms/" + realmName + "/users";
const request = require('request')
const _ = require('lodash')
const jwt = require('jsonwebtoken');
const fs = require('fs');
var async = require('async');
const templates = require('./templates/template.config.json');


const port = process.env.PORT || 8090;

app.use(cors())
app.use(morgan('dev'));
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

app.post("/register/users", (req, res) => {
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


app.post("/registry/add", (req, res, next) => {
    postCallToRegistry(req.body, "/add", function (err, data) {
        return res.send(data);
    });
});

app.post("/registry/search", (req, res, next) => {
    postCallToRegistry(req.body, "/search", function (err, data) {
        return res.send(data);
    });
});

app.post("/registry/read", (req, res, next) => {
    postCallToRegistry(req.body, "/read", function (err, data) {
        return res.send(data);
    })
});

app.post("/registry/update", (req, res, next) => {
    postCallToRegistry(req.body, "/update", function (err, data) {
        return res.send(data);
    })
});

app.get("/formTemplates", (req, res, next) => {
    getFormTemplates(req.headers, function (err, data) {
        if (err) {
            res.statusCode = 404;
            return res.send(err);
        }
        else {
            const json = {
                result: { formTemplate: data },
                responseCode: 'OK'
            }
            return res.send(json)
        }
    })
});

const getFormTemplates = (header, callback) => {
    let roles = [];
    var token =  header['authorization'].replace('Bearer ', '');
    var decoded = jwt.decode(token);
    if (header.role) {
        roles = [header.role]
    } else if (decoded.realm_access) {
        roles = decoded.realm_access.roles;
    }
    readFormTemplate(getTemplateName(roles), function (err, data) {
        if (err) callback(err, null);
        else callback(null, data);
    });
}

/**
 * pick the template according to the role, preferences is ordered 
 * @param {*} roles 
 */
//todo get roles from config
const getTemplateName = (roles) => {
    if (_.includes(roles, 'admin'))
        return templates.formTemplates['admin'];
    if (_.includes(roles, 'partner-admin'))
        return templates.formTemplates['partner-admin'];
    if (_.includes(roles, 'fin-admin'))
        return templates.formTemplates['fin-admin'];
    if (_.includes(roles, 'owner'))
        return templates.formTemplates['owner']
}

const readFormTemplate = (value, callback) => {
    fs.readFile(value, (err, data) => {
        if (err) callback(err, null);
        else {
            let jsonData = JSON.parse(data);
            callback(null, jsonData);
        }
    });
}

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