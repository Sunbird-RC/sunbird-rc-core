const registryHost = process.env.registry_url || "http://localhost:8081";
const httpUtil = require('./httpUtils.js')

const addEmployee = (value, callback) => {
    value['isApproved'] = false;
    let reqBody = {
        "id": "open-saber.registry.create",
        "ver": "1.0",
        "ets": "11234",
        "params": {
            "did": "",
            "key": "",
            "msgid": ""
        },
        "request": {
            "Employee": value
        }
    }
    const options = {
        url: registryHost + "/add",
        headers: {
            'content-type': 'application/json',
            'accept': 'application/json'
        },
        body: reqBody
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
        headers: {
            'content-type': 'application/json',
            'accept': 'application/json'
        },
        body: value
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
        headers: {
            'content-type': 'application/json',
            'accept': 'application/json'
        },
        body: value
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
        headers: {
            'content-type': 'application/json',
            'accept': 'application/json'
        },
        body: value
    }
    httpUtil.post(options, function (err, res) {
        if (res) {
            callback(null, res.body)
        } else {
            callback(err)
        }
    })
}

exports.addEmployee = addEmployee
exports.updateEmployee = updateEmployee
exports.readEmployee = readEmployee
exports.searchEmployee = searchEmployee