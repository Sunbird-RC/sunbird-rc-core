const registryHost = process.env.registry_url || "http://localhost:9080";
const httpUtil = require('./httpUtils.js')

class RegistryService {

    constructor() {
    }

    addRecord(value, callback) {
        const options = {
            url: registryHost + "/add",
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
            url: registryHost + "/update",
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
            url: registryHost + "/read",
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
            url: registryHost + "/search",
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
