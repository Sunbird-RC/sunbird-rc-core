package services

import (
	"encoding/json"
	"errors"

	"github.com/go-resty/resty/v2"
	log "github.com/sirupsen/logrus"
	"github.com/sunbirdrc/notification-service/config"
)

func SendSMS(mobileNumber string, message string) (map[string]interface{}, error) {
	if config.Config.SmsAPI.Enable {
		smsRequest := GetSmsRequestPayload(message, mobileNumber)
		log.Info("SMS request ", config.Config.SmsAPI.URL, smsRequest)
		client := resty.New()
		resp, err := client.R().
			SetHeader("authkey", config.Config.SmsAPI.AuthKey).
			SetHeader("Content-Type", "application/json").
			SetBody(smsRequest).
			Post(config.Config.SmsAPI.URL)
		if err != nil {
			return nil, nil
		}
		if resp.StatusCode() != 200 {
			return nil, errors.New(resp.String())
		}
		responseObject := map[string]interface{}{}
		if err = json.Unmarshal(resp.Body(), &responseObject); err != nil {
			return nil, nil
		}
		log.Infof("Response %+v", responseObject)
		if responseObject["a"] != "SUCCESSFUL" {
			log.Infof("Response %+v", responseObject)
			return nil, nil
		}
		return responseObject, nil
	}
	log.Infof("SMS notifier disabled")
	return nil, nil
}

func GetSmsRequestPayload(message string, mobileNumber string) map[string]interface{} {
	smsRequest := make(map[string]interface{})
	log.Infof("%v", message)
	if err := json.Unmarshal([]byte(message), &smsRequest); err == nil {
		log.Infof("success")
		return smsRequest
	} else {
		log.Errorf("error: %v", err)
	}
	return nil
}
