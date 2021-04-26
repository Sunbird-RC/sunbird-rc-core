let Functions = require("./workflow/Functions");
const _ = require('lodash')
const async = require('async');
const entityType = 'Employee';
const logger = require('./sdk/log4j');
const vars = require('./sdk/vars').getAllVars(process.env.NODE_ENV);


class EPRFunctions extends Functions {
    EPRFunctions() {
        setRequest(undefined)
    }

    /**
     * gets user mail ids from keycloak where user role = admin
     * @param {*} callback 
     */
    getAdminUsers(callback) {
        this.getUsersByRole('admin', (err, data) => {
            this.addEmailToPlaceHolder(data, callback);
        });
    }

    /**
     * gets user mail ids from keycloak where user role = partner-admin
     * @param {*} callback 
     */
    getPartnerAdminUsers(callback) {
        this.getUsersByRole('partner-admin', (err, data) => {
            this.addEmailToPlaceHolder(data, callback);
        })
    }

    /**
     * gets user mail ids from keycloak where user role = fin-admin
     * @param {*} callback 
     */
    getFinAdminUsers(callback) {
        this.getUsersByRole('fin-admin', (err, data) => {
            this.addEmailToPlaceHolder(data, callback);
        });
    }

    /**
     * gets user mail ids from keycloak where user role = reporter
     * @param {*} callback 
     */
    getReporterUsers(callback) {
        this.getUsersByRole('reporter', (err, data) => {
            this.addEmailToPlaceHolder(data, callback);
        });
    }

    /**
     * geets user mail ids from keycloak where user role = owner
     * @param {*} callback 
     */
    getOwnerUsers(callback) {
        this.getUsersByRole('owner', (err, data) => {
            this.addEmailToPlaceHolder(data, callback);
        });
    }

    /**
     * to send notification to Employee after successfully onboarding (both for partner employees and ekstep employess)
     * to get the registry user mail id
     * @param {} callback 
     */
    getRegistryUsersMailId(callback) {
        this.request.body.request['viewTemplateId'] = "6245495a-4745-11ea-b77f-2e728ce88125.json"
        let tempParams = {}
        this.getUserByid((err, data) => {
            if (data) {
                tempParams = data.result[entityType];
                tempParams['employeeName'] = data.result[entityType].name
                tempParams['eprURL'] = vars.appUrl
                if(tempParams.orgName == 'EkStep') {
                    this.addToPlaceholders('templateId', "onboardSuccessTemplateEkstepEmployee");
                } else {
                    this.addToPlaceholders('templateId', "onboardSuccessTemplate");
                }
                this.addToPlaceholders('templateParams', tempParams)
                this.addEmailToPlaceHolder([data.result[entityType]], callback);
            }
        })
    }

    /**
     * gets email from the object and adds to the placeholder
     * @param {object} data 
     * @param {*} callback 
     */
    addEmailToPlaceHolder(data, callback) {
        this.addToPlaceholders('emailIds', _.map(data, 'email'));
        callback();
    }

    /**
     * sends notification to Admin (requesting to onboard new employee)
     * @param {*} callback 
     */
    sendNotificationForRequestToOnBoard(callback) {
        this.addToPlaceholders('subject', "Request to Onboard " + this.request.body.request[entityType].name)
        this.addToPlaceholders('templateId', "requestOnboardTemplate");
        let tempParams = this.request.body.request[entityType];
        tempParams['employeeName'] = this.request.body.request[entityType].name
        tempParams['empRecord'] = vars.appUrl + "/profile/" + JSON.parse(this.response).result[entityType].osid
        this.addToPlaceholders('templateParams', tempParams);
        let actions = ['getAdminUsers', 'sendNotifications'];
        this.invoke(actions, (err, data) => {
            callback(null, data)
        });
    }

    /**
     * sends notification to Employee after successfully onboarding (seed/users)
     * @param {*} callback 
     */
    sendNotificationForSuccessfullyOnboarding(callback) {
        if(this.request.body.request[entityType].isActive) {
            if(this.request.body.request[entityType].orgName == 'EkStep') {
                this.addToPlaceholders('templateId', "onboardSuccessTemplateEkstepEmployee");
            } else {
                this.addToPlaceholders('templateId', "onboardSuccessTemplate");
            }
            this.addToPlaceholders('subject', "Successfully Onboarded to EkStep")
            let tempParams = this.request.body.request[entityType];
            tempParams['employeeName'] = this.request.body.request[entityType].name
            tempParams['eprURL'] = vars.appUrl + "/profile/" + JSON.parse(this.response).result[entityType].osid
            this.addToPlaceholders('templateParams', tempParams);
            this.addToPlaceholders('emailIds', [this.request.body.request[entityType].email])
            let actions = ['sendNotifications'];
            this.invoke(actions, (err, data) => {
                callback(null, data)
            });
        } else {
            callback(null, "Not active user")
        }
    }

    /**
     * sends notification to the Employee if employee succesfully onboarded to organization
     * @param {*} callback 
     */
    sendOnboardSuccesNotification(callback) {
        this.addToPlaceholders('subject', "New Employee Onboarded")
        this.addToPlaceholders('templateId', "newPartnerEmployeeTemplate");
        let actions = ['getRegistryUsersInfo', 
                        'getAdminUsers', 'sendNotifications', 
                        'getReporterUsers', 'sendNotifications',
                        'getFinAdminUsers', 'sendNotifications'];
        this.invoke(actions, (err, data) => {
            callback(null, data)
        });
    }

    /**
     * notify the user who are offboarded form the Ekstep 
     * notify to admins, reporters, 
     * @param {*} callback 
     */
    sendOffboardSuccessNotification(callback) {
        this.addToPlaceholders('subject', "Successfully Offboarded")
        this.addToPlaceholders('templateId', "offboardingSuccessEmployeeTemplate");
        let actions = ['getRegistryUsersInfo',
            'getAdminUsers', 'sendNotifications',
            'getReporterUsers', 'sendNotifications',
            'getFinAdminUsers', 'sendNotifications',
        ];
        this.invoke(actions, (err, data) => {
            callback(null, data)
        });
    }

    /**
     * sends notification to manager to onBoard new Employee (self registeration)
     * @param {*} callback 
     */
    sendNotificationForManager(callback) {
        this.addToPlaceholders('subject', "[Action Required] Validate reportee " + this.request.body.request[entityType].name)
        this.addToPlaceholders('templateId', "supervisorNotificationTemplate");
        let tempParams = this.request.body.request[entityType];
        tempParams['employeeName'] = this.request.body.request[entityType].name
        tempParams['empRecord'] = vars.appUrl + "/actions"
        this.addToPlaceholders('templateParams', tempParams);
        this.addToPlaceholders('emailIds', [this.request.body.request[entityType].manager])
        let actions = ['sendNotifications'];
        this.invoke(actions, (err, data) => {
            callback(null, data)
        });
    }


    /**
     * sends notification to self Registry user
     * @param {*} callback 
     */
    sendNotificationToSelfRegisteredUser(callback) {
        this.addToPlaceholders('subject', "Successfully Registered");
        this.addToPlaceholders('templateId', "newEkStepUserNotificationTemplate");
        let tempParams = this.request.body.request[entityType];
        tempParams['employeeName'] = this.request.body.request[entityType].name
        tempParams['eprURL'] = vars.appUrl
        this.addToPlaceholders('templateParams', tempParams);
        this.addToPlaceholders('emailIds', [this.request.body.request[entityType].email])
        let actions = ['sendNotifications'];
        this.invoke(actions, (err, data) => {
            callback(null, data)
        });
    }


    /**
     * to get Employee deatils(from registry)
     * @param {*} callback 
     */
    getRegistryUsersInfo(callback) {
        let tempParams = {}
        this.getUserByid((err, data) => {
            if (data) {
                tempParams = data.result[entityType];
                tempParams['employeeName'] = data.result[entityType].name
                tempParams['eprURL'] = vars.appUrl
                this.addToPlaceholders('templateParams', tempParams)
                callback()
            }
        })
    }

    /**
     * This method is inovoked to send notiifcations whenever Employee record is updated(for eg attributes like macAddress ,
     * githubId and isOnBoarded are updated)
     * @param {*} callback 
     */
    notifyUsersBasedOnAttributes(callback) {
        let attributesUpdated = _.keys(this.request.body.request[entityType]);//get the list of updated attributes from the req
        let count = 0
        async.forEachSeries(this.notifyAttributes, (param, callback2) => {
            count ++
            if (_.includes(attributesUpdated, param)) {
                let params = {
                    paramName: param,
                    [param]: this.request.body.request[entityType][param]
                }
                this.addToPlaceholders('templateParams', params)
                this.getActions(param, (err, data) => {
                    if (data) {
                        callback2();
                    }
                });
            } else {
                callback2();
            }
            if (count === this.notifyAttributes.length) {
                callback(null, "success")
            }
        });
    }

    /**
     * this function is invoked by notifyUsersBasedOnAttributes function to get the actions to be done(to send notification) on the attribute updated
     * each case contains name of the functions( in the array ), to be invoked for sending notification.
     * @param {*} attribute  param that is updated 
     * @param {*} callback 
     */
    getActions(attribute, callback) {
        let actions = []
        switch (attribute) {
            case 'githubId':
                actions = ['getFinAdminUsers', 'sendNotifications'];
                this.addToPlaceholders('subject', "GitHub Id updation");
                this.addToPlaceholders('templateId', "updateParamTemplate");
                this.invoke(actions, (err, data) => {
                    callback(null, data)
                });
                break;
            case 'macAddress':
                actions = ['getRegistryUsersInfo', 'getReporterUsers', 'sendNotifications'];
                this.addToPlaceholders('subject', "MacAdress updation");
                this.addToPlaceholders('templateId', "macAddressUpdateTemplate");
                this.invoke(actions, (err, data) => {
                    callback(null, data)
                });
                break;
            case 'isOnboarded':
                //if isOnBoarded attribute is set to true, email is sent to Employee (as you are OnBoarded successfully to the Ekstep) and Admin (as new Employee onBoarded)
                if (this.request.body.request[entityType][attribute]) {
                    actions = ['getRegistryUsersMailId', 'sendNotifications', 'sendOnboardSuccesNotification']
                    this.addToPlaceholders('subject', "Successfully Onboarded to EkStep");
                    this.invoke(actions, (err, data) => {
                        callback(null, data)
                    });
                } else {
                    callback(null, "employee deboarded")
                }
                break;
        }
    }


    /**
     * 
     * @param {*} actions array of functions to be called
     * @param {*} callback 
     */
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