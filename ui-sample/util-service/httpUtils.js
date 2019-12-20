const request = require('request');



const post = (req, callback) => {
    let option = {
        json: true,
        headers: req.headers,
        body: req.body,
        url: req.url
    }
    request.post(option, function (err, res) {
        if (res) {
            callback(null, res)
        }
        else {
            callback(err, null);
        }
    });
}


const get = (option, callback) => {
    request.get(option, function (err, res) {
        if (res) {
            callback(null, res)
        }
        else {
            callback(err, null);
        }
    });
}

const getDefaultHeaders = (otherHeaders) => {
    let headers = {
        'content-type': 'application/json',
        'accept': 'application/json'
    }
    return headers;
}

exports.post = post
exports.get = get
