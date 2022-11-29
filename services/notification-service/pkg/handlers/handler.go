package handlers

import (
	"errors"
	"fmt"
	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
	"github.com/sunbirdrc/notification-service/config"
	"github.com/sunbirdrc/notification-service/pkg/services"
	"github.com/sunbirdrc/notification-service/swagger_gen/models"
	"github.com/sunbirdrc/notification-service/swagger_gen/restapi/operations"
	"github.com/sunbirdrc/notification-service/swagger_gen/restapi/operations/health"
	"github.com/sunbirdrc/notification-service/swagger_gen/restapi/operations/notification"
	"net/http"
)

func SetupHandlers(api *operations.NotificationServiceAPI) {
	api.NotificationPostNotificationHandler = notification.PostNotificationHandlerFunc(postNotificationHandler)
	api.NotificationGetNotificationHandler = notification.GetNotificationHandlerFunc(getLastSentNotifications)
	api.HealthGetHealthHandler = health.GetHealthHandlerFunc(getHealthHandle)

}

var messages = make(map[string]string)

func getHealthHandle(params health.GetHealthParams) middleware.Responder {
	response := health.NewGetHealthOK()
	response.Payload = map[string]string{
		"status": "UP",
	}
	return response
}

func getLastSentNotifications(params notification.GetNotificationParams) middleware.Responder {
	response := notification.NewGetNotificationOK()
	if config.Config.TrackNotifications {
		response.Payload = messages
	} else {
		response.Payload = map[string]string{}
	}
	return response
}

func postNotificationHandler(params notification.PostNotificationParams) middleware.Responder {
	requestBody := params.Body
	if mobileNumber, err := services.GetMobileNumber(*requestBody.Recipient); err == nil {
		messages[mobileNumber] = *requestBody.Message
		if response, err := services.SendSMS(mobileNumber, *requestBody.Message); err == nil {
			log.Infof("Successfully sent SMS %+v", response)
			return successResponse("Successfully sent SMS " + fmt.Sprintf("%v", response))
		} else {
			log.Errorf("Failed sending SMS %+v", err)
			return badNotificationRequest(err)
		}
	}
	if emailId, err := services.GetEmailId(*requestBody.Recipient); err == nil {
		if err := services.SendEmail(emailId, requestBody.Subject, *requestBody.Message); err == nil {
			log.Infof("Successfully sent Email %+v")
			return successResponse("Successfully sent Email ")
		} else {
			log.Errorf("Failed sending email %+v", err)
			return badNotificationRequest(err)
		}
	}
	return badNotificationRequest(errors.New("invalid message type"))
}

func badNotificationRequest(err error) middleware.Responder {
	badRequest := notification.NewPostNotificationBadRequest()
	errorStr := fmt.Sprint(err)
	badRequestStatus := fmt.Sprintf("%v", http.StatusBadRequest)
	response := models.Error{
		Code:    &badRequestStatus,
		Message: &errorStr,
	}
	badRequest.SetPayload(&response)
	return badRequest
}

func successResponse(msg string) middleware.Responder {
	successResponse := notification.NewPostNotificationOK()
	badRequestStatus := fmt.Sprintf("%v", http.StatusOK)
	response := models.Success{
		Code:    &badRequestStatus,
		Message: &msg,
	}
	successResponse.SetPayload(&response)
	return successResponse
}
