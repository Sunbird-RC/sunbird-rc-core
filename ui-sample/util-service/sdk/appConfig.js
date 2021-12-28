const appConfig = {
    APP_ID: {
        CREATE: "sunbird-rc.registry.create",
        READ: "sunbird-rc.registry.read",
        UPDATE: "sunbird-rc.registry.update",
        DELETE: "sunbird-rc.registry.delete",
        SEARCH: "sunbird-rc.registry.search"
    },
    UTILS_URL_CONFIG: {
        ADD: "/registry/add",
        READ: "/registry/read",
        UPDATE: "/registry/update",
        SEARCH: "/registry/search",
        USERS: "/register/users",
        NOTIFICATIONS: "/notification"
    },
    STATUS: {
        SUCCESSFULL: "SUCCESSFUL",
        UNSUCCESSFUL: "UNSUCCESSFUL"
    }
}

module.exports = appConfig;