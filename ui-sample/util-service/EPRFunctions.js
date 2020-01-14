let Functions = require("./workflow/Functions");
const _ = require('lodash')
const async = require('async');

class EPRFunctions extends Functions {
    EPRFunctions() {
        setRequest(undefined)
    }

    getAdminUsers(callback) {
        this.getUsersByRole('admin', (err, data) => {
            this.addEmailToPlaceHolder(data, callback);
        });
    }

    getPartnerAdminUsers(callback) {
        this.getUsersByRole('partner-admin', (err, data) => {
            this.addEmailToPlaceHolder(data, callback);
        })
    }

    getFinAdminUsers(callback) {
        this.getUsersByRole('fin-admin', (err, data) => {
            this.addEmailToPlaceHolder(data, callback);
        });
    }

    getReporterUsers(callback) {
        this.getUsersByRole('reporter', (err, data) => {
            this.addEmailToPlaceHolder(data, callback);
        });
    }

    getOwnerUsers(callback) {
        this.getUsersByRole('owner', (err, data) => {
            this.addEmailToPlaceHolder(data, callback);
        });
    }

    getRegistryUsersMailId(callback) {
        this.getUserByid((err, data) => {
            if (data) {
                this.addEmailToPlaceHolder([data], callback);
            }
        })
    }

    addEmailToPlaceHolder(data, callback) {
        this.addToPlaceholders('emailIds', _.map(data, 'email'));
        callback();
    }

    notifyUsersBasedOnAttributes(callback) {
        let params = _.keys(this.request.body.request.Employee);
        async.forEachSeries(this.attributes, (value, callback) => {
            if (_.includes(params, value)) {
                let params = {
                    paramName: value,
                    paramValue: this.request.body.request.Employee[value]
                }
                this.addToPlaceholders('templateParams', params)
                this.getActions(value, (err, data) => {
                    if (data) {
                        callback();
                    }
                });
            } else {
                callback();
            }
        });
    }

    getActions(attribute, callback) {
        let actions = []
        switch (attribute) {
            case 'githubId':
                actions = ['getFinAdminUsers', 'sendNotifications'];
                this.addToPlaceholders('templateId', "updateParamTemplate");
                this.invoke(actions, (err, data) => {
                    callback(null, data)
                });
                break;
            case 'macAddress':
                actions = ['getReporterUsers', 'sendNotifications'];
                this.addToPlaceholders('templateId', "updateParamTemplate");
                this.invoke(actions, (err, data) => {
                    callback(null, data)
                });
                break;
            case 'isOnboarded':
                actions = ['getRegistryUsersMailId', 'sendNotifications'];
                this.addToPlaceholders('templateId', "onboardSuccesstemplate");
                this.invoke(actions, (err, data) => {
                    callback(null, data)
                });
                break;
        }
    }

    invoke(actions, callback) {
        if (actions.length > 0) {
            let count = 0;
            async.forEachSeries(actions, (value, callback2) => {
                count++;
                this[value]((err, data) => {
                    callback2()
                });
                if (count == actions.length) {
                    callback(null, actions);
                }
            });
        }
    }

    searchCheck(callback) {
        console.log("search is hit")
        callback(null)
    }

}

module.exports = EPRFunctions