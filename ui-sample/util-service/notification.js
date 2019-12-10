const httpUtil = require('./httpUtils.js');
const notificationHost = process.env.notificationUrl || "http://localhost:9000/v1/notification/send/sync"
const _ = require('lodash')

const sendNotifications = (mailIds) => {
    const reqBody = {
        id: "notification.message.send",
        ver: "1.0",
        ets: "11234",
        params: {
            "did": "",
            "key": "",
            "msgid": ""
        },
        request: {
            notifications: [
                {
                    mode: "email",
                    deliveryType: "message",
                    config: { "subject": "Welcome to Ekstep" },
                    ids: mailIds,
                    template: {
                        data: "Hello, thanks for completing",
                    }
                }
            ]
        }
    }
    const option = {
        method: 'POST',
        url: notificationHost,
        headers: {
            'content-type': 'application/json',
            'accept': 'application/json'
        },
        body: reqBody,
    }
    httpUtil.post(option, function (err, res) {
        if (res) {
            console.log("notification has been sucessfully sent", res.body)
            // callback(null, res.body)
        } else {
            // callback(err)
        }
    });
}

module.exports = sendNotifications;
