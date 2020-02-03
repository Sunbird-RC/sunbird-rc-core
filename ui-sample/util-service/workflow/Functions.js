const _ = require('lodash')
const async = require('async');

const KeycloakHelper = require('../sdk/KeycloakHelper.js');
const Notification = require('../sdk/Notification.js')
const RegistryService = require('../sdk/RegistryService.js');
const logger = require('../sdk/log4j.js')
var CacheManager = require('../sdk/CacheManager.js');
var vars = require('../sdk/vars').getAllVars(process.env.NODE_ENV);
var appConfig = require('../sdk/appConfig');
var cacheManager = new CacheManager();
const registryService = new RegistryService();
const keycloakHelper = new KeycloakHelper(vars.keycloak);

class Functions {
    constructor() {
        // Provide a property bag for any data exchange between workflow functions.
        this._placeholders = {};

        //list of param names to send notification if any of the attribute is updated
        this.notifyAttributes = ["macAddress", "githubId", "isOnboarded"]
    }

    setRequest(request) {
        this.request = request
    }

    setResponse(response) {
        this.response = response;
    }

    addToPlaceholders(anyKey, anyValue) {
        this._placeholders[anyKey] = anyValue
    }

    getPlaceholders(anyKey) {
        return this._placeholders[anyKey]
    }

    /**
     * 
     * @param {String} roleName 
     * @param {fun(err, data)} callback 
     */
    getUsersByRole(roleName, callback) {
        let tokenDetails;
        this.getTokenDetails((err, token) => {
            if (token) {
                tokenDetails = token;
                keycloakHelper.getUserByRole(roleName, tokenDetails.access_token.token, function (err, data) {
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


    /**
     * used to get registry user
     * @param {*} callback 
     */
    getUserByid(callback) {
        logger.info("get user by id method invoked ", this.request.body)
        let req = {
            headers: this.request.headers,
            body: this.request.body
        };
        req.body.id = appConfig.APP_ID.READ,
            registryService.readRecord(req, (err, data) => {
                if (data) {
                    callback(null, data)
                } else {
                    callback(err);
                }
            });
    }

    /**
     * calls notification send api 
     * @param {*} callback 
     */
    sendNotifications(callback) {
        const notification = new Notification(null, null,
            this._placeholders.templateId, this._placeholders.templateParams, this._placeholders.emailIds, this._placeholders.subject);
        notification.sendNotifications((err, data) => {
            if (data) {
                callback(null, data);
            }
        });
    }

    /**
     * 
     * @param {*} callback 
     */
    getTokenDetails(callback) {
        cacheManager.get('usertoken', function (err, tokenData) {
            if (err || !tokenData) {
                keycloakHelper.getToken(function (err, token) {
                    if (token) {
                        cacheManager.set({ key: 'usertoken', value: { authToken: token } }, function (err, res) { });
                        callback(null, token);
                    } else {
                        callback(err);
                    }
                });
            } else {
                callback(null, tokenData.authToken);
            }
        });
    }

    check123() {
        console.log("Check 123 function invoked - this means we are all set")
    }
}

module.exports = Functions;
