const httpUtil = require('./httpUtils.js');
const notificationHost = process.env.notificationUrl || "http://localhost:9000/v1/notification/send/sync"
const _ = require('lodash')
const logger = require('./log4j.js');

const sendNotifications = (placeholder, callback) => {
    logger.info("email ids to send notifications", placeholder.emailIds);
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
                    ids: placeholder.emailIds,
                    template: {
                        id: placeholder.templateId,
                        params: placeholder.templateParams
                    },
                }
            ]
        }
    }
    logger.info("request body of notification request", JSON.stringify(reqBody));
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
            logger.info("notification has been sucessfully sent", res.body)
            callback(null, res.body)
        } else {
            logger.error("sending notification is unsuccessfull", err)
            callback(err)
        }
    });
}

module.exports = sendNotifications;
