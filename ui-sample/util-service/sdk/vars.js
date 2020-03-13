const config = {
    "dev": {
        "utilServicePort":process.env.util_service_port || 9081,
        "keycloak": {
            "url": process.env.keycloak_url || "http://localhost:8443", 
            "realmName": process.env.keycloak_realmName || "PartnerRegistry",
            "clientId": "utils",
            "clientSecret": process.env.keycloak_clientSecret || "fd637035-cfbd-48de-8fac-073e7e6614b2"
        },
        "keycloak_ner": {
            "url": process.env.keycloak_ner_url || "http://localhost:8443",
            "realmName": process.env.keycloak_ner_realmName || "NIITRegistry",
            "clientId": "utils",
            "clientSecret": process.env.keycloak_ner_clientSecret || "f6ce7466-b04f-4ccf-b986-e9c61e5fb26b"
        },
        "notificationUrl": process.env.notificationUrl || "http://localhost:9012",
        "appUrl": process.env.appUrl || "http://localhost:9082",
        "registryUrl": process.env.registry_url || "http://localhost:9080",
        "nerUtilServiceUrl": process.env.ner_utilservice_url || "http://localhost:9181",
        "notificationShouldSend": process.env.notification_send || false
    },
    "prod": {
        "keycloak": {
            "url": process.env.keycloak_url,
            "realmName": process.env.keycloak_realmName,
            "clientId": "utils",
            "clientSecret": process.env.keycloak_clientSecret
        },
        "keycloak_ner": {
            "url": process.env.keycloak_ner_url,
            "realmName": process.env.keycloak_ner_realmName,
            "clientId": "utils",
            "clientSecret": process.env.keycloak_ner_clientSecret
        },
        "appUrl": process.env.appUrl,
        "notificationUrl": process.env.notificationUrl,
        "appUrl": process.env.appUrl,
        "registryUrl": process.env.registry_url,
        "nerUtilServiceUrl": process.env.ner_utilservice_url,
        "notificationShouldSend": process.env.notification_send || false
    }
}

const logger = require('./log4j')

module.exports.getAllVars = function (envName) {
    var environment = envName

    // FIXME: Dont think config based on env is needed anymore given all 
    // vars can be picked up from env-vars.
    if (envName === undefined) {
        environment = 'dev'
    }
    logger.info("Service running in mode = " + environment)
    return config[environment]
}
