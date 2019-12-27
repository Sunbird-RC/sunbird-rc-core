const registryHost = process.env.registry_url || "http://localhost:8081";
const httpUtil = require('./httpUtils.js')

const addEmployee = (value, callback) => {
    const options = {
        url: registryHost + "/add",
        headers: getDefaultHeaders(value.headers),
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

const updateEmployee = (value, callback) => {
    const options = {
        url: registryHost + "/update",
        headers: getDefaultHeaders(value.headers),
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

const readEmployee = (value, callback) => {
    const options = {
        url: registryHost + "/read",
        headers: getDefaultHeaders(value.headers),
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

const searchEmployee = (value, callback) => {
    const options = {
        url: registryHost + "/search",
        headers: getDefaultHeaders(value.headers),
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

const getDefaultHeaders = (reqHeaders) => {
    var token = reqHeaders.authorization.replace('Bearer ', '');
    let headers = {
        'content-type': 'application/json',
        'accept': 'application/json',
        'x-authenticated-user-token': token
    }
    return headers;
}

exports.addEmployee = addEmployee
exports.updateEmployee = updateEmployee
exports.readEmployee = readEmployee
exports.searchEmployee = searchEmployee