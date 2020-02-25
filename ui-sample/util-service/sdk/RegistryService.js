const vars = require('./vars').getAllVars(process.env.NODE_ENV)
const registryUrl = vars['registryUrl']
const httpUtil = require('./httpUtils.js')

class RegistryService {

    constructor() {
    }

    addRecord(value, callback) {
        const options = {
            url: registryUrl + "/add",
            headers: this.getDefaultHeaders(value.headers),
            body: value.body
        }
        httpUtil.post(options, function (err, res) {
            if (res) {
                callback(null, res)
            } else {
                callback(err)
            }
        });

    }

    updateRecord(value, callback) {
        const options = {
            url: registryUrl + "/update",
            headers: this.getDefaultHeaders(value.headers),
            body: value.body
        }
        httpUtil.post(options, function (err, res) {
            if (res) {
                callback(null, res.body)
            } else {
                callback(err)
            }
        })

    }

    readRecord(value, callback) {
        const options = {
            url: registryUrl + "/read",
            headers: this.getDefaultHeaders(value.headers),
            body: value.body
        }
        httpUtil.post(options, function (err, res) {
            if (res) {
                callback(null, res.body)
            } else {
                callback(err)
            }
        })
    }

    searchRecord(value, callback) {
        const options = {
            url: registryUrl + "/search",
            headers: this.getDefaultHeaders(value.headers),
            body: value.body
        }
        httpUtil.post(options, function (err, res) {
            if (res) {
                callback(null, res.body)
            } else {
                callback(err)
            }
        })
    }

    searchAuditRecords(value, callback) {
        const options = {
            url: registryUrl + "/audit",
            headers: this.getDefaultHeaders(value.headers),
            body: value.body
        }
        httpUtil.post(options, function (err, res) {
            if (res) {
                callback(null, res.body)
            } else {
                callback(err)
            }
        })
    }

    getDefaultHeaders(reqHeaders) {
        var token = reqHeaders.authorization.replace('Bearer ', '');
        let headers = {
            'content-type': 'application/json',
            'accept': 'application/json',
            'x-authenticated-user-token': token
        }
        return headers;
    }
}


module.exports = RegistryService;
