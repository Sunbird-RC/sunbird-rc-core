const keycloakHelper = require('../sdk/keycloakHelper.js');
const registryService = require('../sdk/registryService.js');
const _ = require('lodash')
const notify = require('../sdk/notification.js')
var async = require('async');
const logger = require('../sdk/log4j.js')


class Functions {

    constructor(request) {
        // Provide access to the request object to all actions.
        this.request = request;

        // Provide a property bag for any data exchange between workflow functions.
        this.placeholders = {};

        this.attributes = ["macAddress", "githubId", "isOnboarded"]
    }

    getAdminUsers(callback) {
        logger.info("get admin users method invoked")
        this.getUserMailId('admin', (err, data) => {
            if (data) {
                let emailIds = [];
                if (data.length > 0) {
                    _.forEach(data, (value) => {
                        emailIds.push(value.email);
                    });
                    this.placeholders['emailIds'] = emailIds;
                    callback(null, data)
                }
            } else {
                callback(err)
            }
        });
    }

    getPartnerAdminUsers(callback) {
        logger.info("get partner-admin users method invoked")
        this.getUserMailId('partner-admin', (err, data) => {
            if (data) {
                let emailIds = [];
                if (data.length > 0) {
                    _.forEach(data, (value) => {
                        emailIds.push(value.email);
                    });
                    this.placeholders['emailIds'] = emailIds;
                    callback(null, data)
                }
            } else {
                callback(err)
            }
        });
    }

    getFinAdminUsers(callback) {
        logger.info("get fin-admin users method invoked")
        this.getUserMailId('fin-admin', (err, data) => {
            if (data) {
                let emailIds = [];
                if (data.length > 0) {
                    _.forEach(data, (value) => {
                        emailIds.push(value.email);
                    });
                    this.placeholders['emailIds'] = emailIds;
                    callback(null, data)
                }
            } else {
                callback(err)
            }
        });
    }
    getReporterUsers(callback) {
        logger.info("get reporter users method invoked")
        this.getUserMailId('reporter', (err, data) => {
            if (data) {
                let emailIds = [];
                if (data.length > 0) {
                    _.forEach(data, (value) => {
                        emailIds.push(value.email);
                    });
                    this.placeholders['emailIds'] = emailIds;
                    callback(null, data)
                }
            } else {
                callback(err)
            }
        });
    }
    getOwnerUsers(callback) {
        logger.info("get owner users method invoked")
        this.getUserMailId('owner', (err, data) => {
            if (data) {
                let emailIds = [];
                if (data.length > 0) {
                    _.forEach(data, (value) => {
                        emailIds.push(value.email);
                    });
                    this.placeholders['emailIds'] = emailIds;
                    callback(null, data)
                }
            } else {
                callback(err)
            }
        });
    }

    getUserByid(callback) {
        logger.info("get user by id method invoked ", this.request.body)
        let req = {};
        req.body = this.request.body;
        req.body.id = "open-saber.registry.read"
        req.body.request.Employee.osid = this.request.body.request.Employee.osid;
        req.headers = this.request.headers;
        registryService.readEmployee(req, (err, data) => {
            if (data) {
                this.userData = data.result.Employee;
                this.placeholders['emailIds'] = [data.result.Employee.email]
                this.getTemplateparams();
                callback(null, data.result.Employee)
            }
        });
    }

    /**
     * calls notification send api 
     * @param {*} callback 
     */
    sendNotifications(callback) {
        this.getTemplateparams();
        logger.info("send notifications");
        notify(this.placeholders, (err, data) => {
            if (data) {
                callback(null, data);
            }
        });
    }

    getTemplateparams(callback) {
        logger.info("get template params");
        let params = {};
        params = this.userData;
        this.placeholders.templateParams = params;
        this.placeholders.templateParams.paramName = this.placeholders.paramName;
        this.placeholders.templateParams.paramValue = this.placeholders.paramValue;
    }

    onBoardNewUserTemplate(callback) {
        logger.info("get onBoardNewUserTemplate template params");
        this.placeholders.templateParams = this.request.body.request;
        this.placeholders.templateId = "newUserOnboard";
    }

    notifyUsersBasedOnAttributes(callback) {
        logger.info("notifiy user based on attribute updated");
        let params = _.keys(this.request.body.request.Employee);
        async.forEachSeries(this.attributes, (value, callback) => {
            if (_.includes(params, value)) {
                this.placeholders.paramName = value
                this.placeholders.paramValue = this.request.body.request.Employee[value];
                this.getActions(value, (err, data) => {
                    if (data) {
                        callback();
                    }});
            } else{
                callback();
            }
        });
    }

    getActions(attribute, callback) {
        logger.info("calling get actions function", attribute);
        let actions = []
        switch (attribute) {
            case 'githubId':
                actions = ['getUserByid', 'sendNotifications', 'getFinAdminUsers', 'sendNotifications'];
                this.placeholders.templateId = "updateParamTemplate";
                this.invoke(actions, (err, data) => {
                    callback(null, data)
                });
                break;
            case 'macAddress':
                actions = ['getReporterUsers', 'sendNotifications'];
                this.placeholders.templateId = "updateParamTemplate";
                this.invoke(actions, (err, data) => {
                    callback(null, data)
                });
                break;
            case 'isOnboarded':
                actions = ['getUserByid', 'sendNotifications', 'getAdminUsers', 'sendNotifications'];
                this.placeholders.templateId = "onboardSuccesstemplate";
                this.invoke(actions, (err, data) => {
                    callback(null, data)
                });
                break;
        }
    }

    invoke(actions, callback2) {
        if (actions.length > 0) {
            let count = 0;
            async.forEachSeries(actions, (value, callback) => {
                count++;
                this[value]((err, data) => {
                    callback()
                });
                if (count == actions.length) {
                    callback2(null, actions);
                }
            });
        }
    }

    getUserMailId(role, callback) {
        let tokenDetails;
        this.getTokenDetails((err, token) => {
            if (token) {
                tokenDetails = token;
                keycloakHelper.getUserByRole(role, tokenDetails.access_token.token, function (err, data) {
                    if (data) {
                        callback(null, data)
                    }
                    else {
                        callback(err)
                    }
                });
            } else {
                callback(err);
            }
        });

    }

    getTokenDetails(callback) {
        keycloakHelper.getToken(function (err, token) {
            if (token) {
                callback(null, token);
            } else {
                callback(err)
            }
        });
    }

}

module.exports = Functions;
