const keycloakHelper = require('../sdk/keycloakHelper.js');
const _ = require('lodash')
const notify = require('../sdk/notification.js')
var async = require('async');


class Functions {

    constructor(request) {
        // Provide access to the request object to all actions.
        this.request = request;

        // Provide a property bag for any data exchange between workflow functions.
        this.placeholders = {};

        this.attributes = ["macAddress", "githubId"]
    }
    
    getAdminUsers(callback) {
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

    /**
     * calls notification send api 
     * @param {*} callback 
     */
    sendNotifications(callback) {
        notify(this.placeholders.emailIds);
    }


    notifyUsersBasedOnAttributes(callback) {
        let params = _.keys(this.request.body.request.Employee);
        _.forEach(this.attributes, (value) => {
            if (_.includes(params, value)) {
                this.getActions(value);
            }
        });
    }

    getActions(attribute, callback) {
        let actions = []
        switch (attribute) {
            case 'githubId':
                actions = ['getFinAdminUsers', 'sendNotifications'];
                this.invoke(actions)
                break;
            case 'macAddress':
                actions = ['getReporterUsers', 'sendNotifications'];
                this.invoke(actions)
                break;
            default:
                callback('no attribute found')
        }
    }

    invoke(actions, callback) {
        if (actions.length > 0) {
            async.forEachSeries(actions, (value, callback) => {
                this[value]((err, data) => {
                    callback()
                })
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
