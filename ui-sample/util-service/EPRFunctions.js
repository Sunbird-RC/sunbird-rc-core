let Functions = require("./workflow/Functions")

class EPRFunctions extends Functions {
    EPRFunctions() {
        setRequest(undefined)
        
    }

    getAdminUsers(callback) {
        this.getUsersByRole('admin', callback)
    }

    getPartnerAdminUsers(callback) {
        this.getUsersByRole('partner-admin', callback)
    }

    getFinAdminUsers(callback) {
        this.getUsersByRole('fin-admin', callback)
    }
    
    getReporterUsers(callback) {
        this.getUsersByRole('reporter', callback)
    }

    getOwnerUsers(callback) {
        this.getUsersByRole('owner', callback)
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

    searchCheck(callback) {
        console.log("search is hit")
        callback(null)
    }

}

module.exports = EPRFunctions