package services

import (
	"encoding/json"
	"errors"

	"github.com/imroc/req"
	log "github.com/sirupsen/logrus"
	"github.com/sunbirdrc/notification-service/config"
)

func SendSMS(mobileNumber string, message string) (map[string]interface{}, error) {
	if config.Config.SmsAPI.Enable {
		smsRequest := GetSmsRequestPayload(message, mobileNumber)
		header := req.Header{
			"authkey":      config.Config.SmsAPI.AuthKey,
			"Content-Type": "application/json",
		}
		log.Info("SMS request ", config.Config.SmsAPI.URL, header, smsRequest)
		response, err := req.Post(config.Config.SmsAPI.URL, header, req.BodyJSON(smsRequest))
		if err != nil {
			return nil, nil
		}
		if response.Response().StatusCode != 200 {
			responseStr, _ := response.ToString()
			return nil, errors.New(responseStr)
		}
		responseObject := map[string]interface{}{}
		err = response.ToJSON(&responseObject)
		if err != nil {
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
