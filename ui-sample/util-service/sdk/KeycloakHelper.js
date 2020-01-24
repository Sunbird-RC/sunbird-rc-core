var keyCloakAuthUtils = require('keycloak-auth-utils');
const httpUtil = require('./httpUtils.js');

class KeycloakHelper {

    /**
     * @param {object} config : keycloak config should contain four params url, realmName, clientId, clientSecret
     */
    constructor(config) {
        this.realmName = config.realmName;
        this.keyCloakHost = config.url;
        this.keyCloak_config = {
            realm: config.realmName,
            "auth-server-url": config.url + "/auth",
            credentials: {
                secret: config.clientSecret
            },
            bearerOnly: true,
            clientId: config.clientId
        }
    }

    async getToken(callback) {
        this.keyCloakConfig = new keyCloakAuthUtils.Config(this.keyCloak_config);
        this.grantManager = new keyCloakAuthUtils.GrantManager(this.keyCloakConfig);
        try {
            let grant = await this.grantManager.obtainFromClientCredentials(undefined, 'openid');
            return callback(null, grant);
        } catch (error) {
            console.log("error", error)
        }
    }

    /**
     * @param {*} role to  get the users are in given role
     * @param {*} token authorization token 
     * @param {*} callback 
     */
    getUserByRole(role, token, callback) {
        var headers = {
            'content-type': 'application/json',
            authorization: 'Bearer ' + token
        }
        try {
            const options = {
                method: 'GET',
                url: this.keyCloakHost + "/auth/admin/realms/" + this.realmName + '/roles/' + role + '/users',
                json: true,
                headers: headers
            }
            httpUtil.get(options, function (err, res, body) {
                if (res.body && res.statusCode == 200) {
                    callback(null, res.body)
                } else {
                    callback(err)
                }
            });
        } catch (err) {

        }
    }

    /**
     * 
     * @param {*} req 
     * @param {*} callback 
     */
    registerUserToKeycloak(req, callback) {
        const value = req.body.request;
        const options = {
            url: this.keyCloakHost + "/auth/admin/realms/" + this.realmName + "/users",
            headers: {
                'content-type': 'application/json',
                accept: 'application/json',
                Authorization: req.headers.authorization
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
                        value: "password",
                        type: "password"
                    }
                ]
            }
        }
        httpUtil.post(options, function (err, res) {
            console.log("success", res)
            callback(null, req, res)
        });
    }

}

module.exports = KeycloakHelper;
