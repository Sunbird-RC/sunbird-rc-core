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
var interceptor = require('express-interceptor');
const templateConfig = require('./templates/template.config.json');
const RegistryService = require('./sdk/RegistryService.js')
const logger = require('./sdk/log4j.js');
const vars = require('./sdk/vars.js').getAllVars(process.env.NODE_ENV);
const port = vars.utilServicePort;
let wfEngine = undefined
const registryService = new RegistryService();

app.use(cors())
app.use(morgan('dev'));
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

const workFlowFunctionPre = (req) => {
    wfEngine.preInvoke(req);
}

const workFlowFunctionPost = (req, res) => {
    wfEngine.postInvoke(req, res);
}

app.use((req, res, next) => {
    logger.info('pre api request interceptor');
    workFlowFunctionPre(req);
    next();
});

app.use(interceptor(function (req, res) {
    return {
        isInterceptable: function () {
            return true;
        },
        intercept: (body, send) => {
            send(body)
        },
        afterSend(oldResBody, newResBody) {
            workFlowFunctionPost(req, newResBody)
        }
    }
}));

app.post("/registry/add", (req, res, next) => {
    registryService.addRecord(req, function (err, data) {
        return res.send(data.body);
    })
});

app.post("/registry/search", (req, res, next) => {
    if (!_.isEmpty(req.headers.authorization)) {
        req.body.request.viewTemplateId = getViewtemplate(req.headers.authorization);
    }
    registryService.searchRecord(req, function (err, data) {
        return res.send(data);
    })
});

app.post("/registry/read", (req, res, next) => {
    registryService.readRecord(req, function (err, data) {
        return res.send(data);
    })
});

app.post("/registry/audit", (req, res, next) => {
    registryService.searchAuditRecords(req, function (err, data) {
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
    registryService.updateRecord(req, function (err, data) {
        if (data) {
            return res.send(data);
        } else {
            return res.send(err);
        }
    })
});

app.post("/notification", (req, res, next) => {
    logger.info("Got notification from external party" + JSON.stringify(req.body))
    registryService.updateRecord(req, function (err, data) {
        if (data) {
            return res.send(data);
        } else {
            return res.send(err);
        }
    });
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

app.get("/formTemplate/:role", (req, res, next) => {
    readFormTemplate(templateConfig.formTemplates[req.params.role], function (err, data) {
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
    var templateName = getTemplateName(roles, 'formTemplates')
    readFormTemplate(templateName, function (err, data) {
        if (err) callback(err, null);
        else callback(null, data);
    })
}

/**
 * pick the template according to the role, preferences is ordered 
 * @param {*} roles 
 */
//todo get roles from config
const getTemplateName = (roles, templateName) => {
    logger.debug("Roles for the current user is - " + roles)
    var highestRole = undefined
    if (_.includes(roles, templateConfig.roles.admin)) {
        highestRole = templateConfig.roles.admin
    } else if (_.includes(roles, templateConfig.roles.partnerAdmin)) {
        highestRole = templateConfig.roles.partnerAdmin
    } else if (_.includes(roles, templateConfig.roles.finAdmin)) {
        highestRole = templateConfig.roles.finAdmin
    } else if (_.includes(roles, templateConfig.roles.reporter)) {
        highestRole = templateConfig.roles.reporter
    } else if (_.includes(roles, templateConfig.roles.owner)) {
        highestRole = templateConfig.roles.owner
    }
    logger.debug("Returning with highestRole = " + highestRole)
    return templateConfig[templateName][highestRole]
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

const setEngine = (engine) => {
    wfEngine = engine
}

module.exports.startServer = (engine) => {
    setEngine(engine)

    server.listen(port, function () {
        logger.info("util service listening on port " + port);
    })
};

// Expose the app object for adopters to add new endpoints.
module.exports.theApp = app
