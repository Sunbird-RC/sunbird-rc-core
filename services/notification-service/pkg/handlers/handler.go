package handlers

import (
	"errors"
	"fmt"
	"github.com/sunbirdrc/notification-service/pkg/services"
	"github.com/sunbirdrc/notification-service/swagger_gen/models"
	"github.com/sunbirdrc/notification-service/swagger_gen/restapi/operations"
	"github.com/sunbirdrc/notification-service/swagger_gen/restapi/operations/notification"
	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
	"net/http"
)

func SetupHandlers(api *operations.NotificationServiceAPI) {
	api.NotificationPostNotificationHandler = notification.PostNotificationHandlerFunc(postNotificationHandler)
	api.NotificationGetNotificationHandler = notification.GetNotificationHandlerFunc(getLastSentNotifications)

}
var messages = make(map[string]string)

func getLastSentNotifications(params notification.GetNotificationParams) middleware.Responder  {
	response := notification.NewGetNotificationOK()
	response.Payload = messages
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
