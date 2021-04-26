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
            url: this.keyCloakHost + "/auth/realms/" + this.realmName + "/users/add",
            headers: {
                'Content-type': 'application/json',
                'Accept': 'application/json',
                'Authorization': req.headers.authorization
            },
            body: {
                username: value.name + Date.now(),
                enabled: true,
                emailVerified: value.emailVerified,
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
        console.log("This is the request going to kc" + JSON.stringify(options.body))
        httpUtil.post(options, function (err, res) {
            console.log("This is the response from KC" + err + " : " + res)
            callback(null, res)
        });
    }

    getUserById(userId, token, callback) {
        var headers = {
            'content-type': 'application/json',
            authorization: token
        }
        try {
            const options = {
                method: 'GET',
                url: this.keyCloakHost + "/auth/admin/realms/" + this.realmName + '/users/' + userId,
                json: true,
                headers: headers
            }
            httpUtil.get(options, function (err, res, body) {
                if (res.body) {
                    callback(null, res)
                } else {
                    callback(err)
                }
            });
        } catch (err) {

        }
    }
    getUserByEmailId(emailId, token, callback) {
        var headers = {
            'content-type': 'application/json',
            authorization: token
        }
        try {
            const options = {
                method: 'GET',
                url: this.keyCloakHost + "/auth/admin/realms/" + this.realmName + '/users?email=' + emailId,
                json: true,
                headers: headers
            }
            httpUtil.get(options, function (err, res, body) {
                if (res.body) {
                    callback(null, res)
                } else {
                    callback(err)
                }
            });
        } catch (err) {

        }
    }

    deleteUserById(req, callback) {
        
        var headers = {
            'content-type': 'application/json',
            authorization: req.headers['authorization']
        }
        try {
            const options = {
                method: 'DELETE',
                url: this.keyCloakHost + "/auth/admin/realms/" + this.realmName + '/users/' + req.body.request.keyCloakId,
                json: true,
                headers: headers
            }
            httpUtil.deletes(options, function (err, res, body) {
                if (res) {
                    callback(null, res)
                } else {
                    callback(err)
                }
            });
        } catch (err) {

        }
    }

    disableUserById(req, callback) {
        
        var headers = {
            'content-type': 'application/json',
            authorization: req.headers['authorization']
        }
        try {
            const options = {
                method: 'PUT',
                url: this.keyCloakHost + "/auth/admin/realms/" + this.realmName + '/users/' + req.body.request.keyCloakId,
                json: true,
                headers: headers,
                body: {
                    enabled: false,
                }
            }
            httpUtil.put(options, function (err, res, body) {
                if (res) {
                    callback(null, res)
                } else {
                    callback(err)
                }
            });
        } catch (err) {

        }
    }

    addUserRoleById(keyCloackUserId, req, callback) {
        
        var headers = {
            'content-type': 'application/json',
            authorization: req.headers['authorization']
        }
        try {
            const options = {
                method: 'POST',
                url: this.keyCloakHost + "/auth/admin/realms/" + this.realmName + '/users/' + keyCloackUserId+'/role-mappings/realm',
                json: true,
                headers: headers,
                body: req.body
            }
            httpUtil.post(options, function (err, res, body) {
                if (res) {
                    callback(null, res)
                } else {
                    callback(err)
                }
            });
        } catch (err) {

        }
    }

    deleteUserRoleById(keyCloackUserId, req, callback) {
        
        var headers = {
            'content-type': 'application/json',
            authorization: req.headers['authorization']
        }
        try {
            const options = {
                method: 'DELETE',
                url: this.keyCloakHost + "/auth/admin/realms/" + this.realmName + '/users/' + keyCloackUserId+'/role-mappings/realm',
                json: true,
                headers: headers,
                body: req.body
            }
            httpUtil.deletes(options, function (err, res, body) {
                if (res) {
                    callback(null, res)
                } else {
                    callback(err)
                }
            });
        } catch (err) {

        }
    }

    /**
     * @param {*} token authorization token 
     * @param {*} callback 
     */
    getRolesByRealm(token, callback) {
        var headers = {
            'content-type': 'application/json',
            authorization: token
        }
        try {
            const options = {
                method: 'GET',
                url: this.keyCloakHost + "/auth/admin/realms/" + this.realmName + '/roles',
                json: true,
                headers: headers
            }
            httpUtil.get(options, function (err, res) {
                if (res) {
                    callback(null, res.body)
                } else {
                    callback(err)
                }
            });
        } catch (err) {

        }
    }
    
}

module.exports = KeycloakHelper;
