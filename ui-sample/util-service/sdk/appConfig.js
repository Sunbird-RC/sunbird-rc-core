const appConfig = {
    APP_ID: {
        CREATE: "open-saber.registry.create",
        READ: "open-saber.registry.read",
        UPDATE: "open-saber.registry.update",
        DELETE: "open-saber.registry.delete",
        SEARCH: "open-saber.registry.search"
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