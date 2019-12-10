const express = require("express");
const http = require("http");
const app = express();
var bodyParser = require("body-parser");
var cors = require("cors")
const morgan = require("morgan");
const server = http.createServer(app);
const _ = require('lodash')
const jwt = require('jsonwebtoken');
const fs = require('fs');
var async = require('async');
const templateConfig = require('./templates/template.config.json');
let notification = require('./notification.js')
const registryService = require('./registryService.js')
const keycloakHelper = require('./keycloakHelper.js');
const notificationRules = require('./notifyRulesSet.json')
app.use(cors())
app.use(morgan('dev'));
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

const port = process.env.PORT || 8090;

app.post("/register/users", (req, res) => {
    createUser(req.body, req.headers, function (err, data) {
        if (err) {
            res.statusCode = err.statusCode;
            return res.send(err.body)
        } else {
            notify(notificationRules.create.role)
            return res.send(data);
        }
    });
});

const createUser = (value, header, callback) => {
    async.waterfall([
        function (callback) {
            keycloakHelper.registerUserToKeycloak(value.request, header, callback)
        },
        function (value, header, res, callback2) {
            console.log("Employee successfully added to registry")
            addEmployeeToRegistry(value, header, res, callback2)
        }
    ], function (err, result) {
        console.log('Main Callback --> ' + result);
        if (err) {
            callback(err, null)
        } else {
            callback(null, result);
        }
    });
}

const addEmployeeToRegistry = (value, header, res, callback) => {
    if (res.statusCode == 201) {
        registryService.addEmployee(value, function (err, res) {
            if (res.statusCode == 200) {
                console.log("Employee successfully added to registry")
                callback(null, res.body)
            } else {
                console.log("Employee could not be added to registry" + res.statusCode)
                callback(res.statusCode, res.errorMessage)
            }
        })
    } else {
        callback(res, null)
    }
}

app.post("/registry/add", (req, res, next) => {
    registryService.addEmployee(req.body, function (err, data) {
        return res.send(data);
    })
});

app.post("/registry/search", (req, res, next) => {
    if (!_.isEmpty(req.headers.authorization)) {
        req.body.request.viewTemplateId = getViewtemplate(req.headers.authorization);
    }
    registryService.searchEmployee(req.body, function (err, data) {
        return res.send(data);
    })
});

app.post("/registry/read", (req, res, next) => {
    registryService.readEmployee(req.body, function (err, data) {
        return res.send(data);
    })
});

const getViewtemplate = (authToken) => {
    var roles = [];
    let token = authToken.replace('Bearer ', '');
    var decoded = jwt.decode(token);
    if (decoded != null && decoded.realm_access) {
        roles = decoded.realm_access.roles;
    }
    var searchTemplate = getTemplateName(roles, 'searchTemplates');
    return searchTemplate;
}

app.post("/registry/update", (req, res, next) => {
    registryService.updateEmployee(req.body, function (err, data) {
        if (data) {
            notifyUserBasedOnAttributes(req);
            return res.send(data);
        } else {
            return res.send(err);
        }
    })
});

const notifyUserBasedOnAttributes = (req) => {
    let params = _.keys(req.body.request.Employee);
    _.forEach(notificationRules.update.attributes, function (value) {
        if (_.includes(params, value)) {
            let roles = notificationRules.update[value].role;
            notify(roles);
        }
    });
}

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

app.get("/owner/formTemplate", (req, res, next) => {
    readFormTemplate(templateConfig.formTemplates.owner, function (err, data) {
        if (err) {
            res.statusCode = 404;
            return res.send(err);
        } else {
            const json = {
                result: { formTemplate: data },
                responseCode: 'OK'
            }
            return res.send(json)
        }
    });
})

const getFormTemplates = (header, callback) => {
    let roles = [];
    var token = header['authorization'].replace('Bearer ', '');
    var decoded = jwt.decode(token);
    if (header.role) {
        roles = [header.role]
    } else if (decoded.realm_access) {
        roles = decoded.realm_access.roles;
    }
    readFormTemplate(getTemplateName(roles, 'formTemplates'), function (err, data) {
        if (err) callback(err, null);
        else callback(null, data);
    });
}

/**
 * pick the template according to the role, preferences is ordered 
 * @param {*} roles 
 */
//todo get roles from config
const getTemplateName = (roles, templateName) => {
    if (_.includes(roles, templateConfig.roles.admin))
        return templateConfig[templateName][templateConfig.roles.admin];
    if (_.includes(roles, templateConfig.roles.partnerAdmin))
        return templateConfig[templateName][templateConfig.roles.partnerAdmin];
    if (_.includes(roles, templateConfig.roles.finAdmin))
        return templateConfig[templateName][templateConfig.roles.finAdmin];
    if (_.includes(roles, templateConfig.roles.owner))
        return templateConfig[templateName][templateConfig.roles.owner]
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

const notify = (roles) => {
    getUserMailId(roles);
}

const getUserMailId = (roles) => {
    let tokenDetails;
    let emailIds = [];
    getTokenDetails(function (err, token) {
        if (token) {
            tokenDetails = token;
            for (let i = 0; i < roles.length; i++) {
                getUser(roles[i], tokenDetails, function (err, data) {
                    if (data.length > 0) {
                        emailIds.push(...data)
                        if (i == roles.length - 1) {
                            let unique = _.uniq(emailIds);
                            notification(unique);
                        }
                    }
                });
            }

        }
    });
}

const getTokenDetails = (callback) => {
    keycloakHelper.getToken(function (err, token) {
        if (token) {
            callback(null, token);
        } else {
            callback(err)
        }
    });
}

function getUser(value, tokenDetails, callback) {
    let emailIds = [];
    keycloakHelper.getUserByRole(value, tokenDetails.access_token.token, function (err, data) {
        if (data) {
            _.forEach(data, function (value) {
                emailIds.push(value.email);
            });
            callback(null, emailIds)
        }
        else {
            callback(err)
        }
    });
}

startServer = () => {
    server.listen(port, function () {
        console.log("util service listening on port " + port);
    })
};

startServer();