
var keyCloakAuthUtils = require('keycloak-auth-utils');
let request = require('request')
const httpUtil = require('./httpUtils.js');
const realmName = process.env.keycloak_realmName || "PartnerRegistry"
const keyCloakHost = process.env.keycloak_url || "http://localhost:8443" + "/auth/admin/realms/" + realmName;

let keyCloak_config = {
    "realm": realmName,
    "auth-server-url": process.env.keycloak_url || "http://localhost:8443" + "/auth",
    "credentials": {
        "secret": "9ebc2fc1-ced9-4774-a661-7e2c59991cfe"
    },
    "resource": "utils",
    "bearerOnly": true,
    "clientId": "utils"
};


const getToken = async (callback) => {
    let adminId = process.env.systemAdminId || "sysadmin@ekstep.org";
    let adminPassword = process.env.systemAdminPassword || "password1";
    this.config = keyCloak_config;
    this.keyCloakConfig = new keyCloakAuthUtils.Config(this.config);
    this.grantManager = new keyCloakAuthUtils.GrantManager(this.keyCloakConfig);
    try {
        let grant = await this.grantManager.obtainDirectly(adminId, adminPassword, undefined, 'openid')
        return callback(null, grant);
    } catch (error) {
        console.log("error", error)
    }
}

const getUserByRole = function (role, token, callback) {

    var headers = {
        'content-type': 'application/json',
        'authorization': 'Bearer ' + token
    }
    try {
        const options = {
            method: 'GET',
            url: keyCloakHost + '/roles/' + role + '/users',
            json: true,
            headers: headers
        }
        request(options, function (err, res, body) {
            if (res.body && res.statusCode == 200) {
                callback(null, res.body)
            } else {
                callback(err)
            }
        });
    } catch (err) {

    }
}

const registerUserToKeycloak = (req, callback) => {
    const value = req.body.request;
    const options = {
        url: keyCloakHost + "/users",
        headers: {
            'content-type': 'application/json',
            'accept': 'application/json',
            'Authorization': req.headers.authorization
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
        }
    }
    httpUtil.post(options, function (err, res) {
        callback(null, req, res)
    });

}

exports.getToken = getToken;
exports.getUserByRole = getUserByRole;
exports.registerUserToKeycloak = registerUserToKeycloak;
