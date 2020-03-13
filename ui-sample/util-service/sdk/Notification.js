const httpUtil = require('./httpUtils.js');
const vars = require('./vars').getAllVars(process.env.NODE_ENV)
const notificationEndPoint = vars['notificationUrl'] + "/v1/notification/send/sync"
const shouldSendNotification = vars['notificationShouldSend']
const _ = require('lodash')
const logger = require('./log4j.js');


class Notification {
    /**
     * 
     * @param {string} mode in which notification sent (for example email, phone or device).
     * @param {string} deliveryType can be message, otp, whatsapp or call
     * @param {string} subject - subject of the mail
     * @param {string} templateId - preexist vm template file name
     * @param {object} templateParams - dynamic data for params present in the given templateId
     * @param {Array} ids to which notification is to be sent.
     */
    constructor(mode, deliveryType, templateId, templateParams, ids, subject) {
        this.deliveryType = deliveryType ? deliveryType : 'message';
        this.mode = mode ? mode : 'email';
        this.templateId = templateId;
        this.templateParams = templateParams;
        this.ids = ids;
        this.subject = subject;
    }

    sendNotifications(callback) {
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
                        mode: this.mode,
                        deliveryType: this.deliveryType,
                        config: { subject: this.subject },
                        ids: this.ids,
                        template: {
                            id: this.templateId,
                            params: this.templateParams
                        },
                    }
                ]
            }
        }
        const option = {
            method: 'POST',
            url: notificationEndPoint,
            headers: {
                'content-type': 'application/json',
                'accept': 'application/json'
            },
            body: reqBody,
        }

        if (shouldSendNotification) {
            httpUtil.post(option, function (err, res) {
                if (res) {
                    logger.info("notification has been sucessfully sent", res.body)
                    callback(null, res.body)
                } else {
                    logger.error("sending notification is unsuccessfull", err)
                    callback(err)
                }
            });
        } else {
            logger.info("DRYRUN WARNING: Notifications not send to " + this.ids + " with subject " + this.subject)
            callback(null, "done")
        }
    }
}


module.exports = Notification;
