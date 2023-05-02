package handlers

import (
	"digilocker_certificate_api/swagger_gen/models"
	"digilocker_certificate_api/swagger_gen/restapi/operations"
	"digilocker_certificate_api/swagger_gen/restapi/operations/health"
	"digilocker_certificate_api/swagger_gen/restapi/operations/pull_certificates"
	"encoding/xml"

	log "github.com/sirupsen/logrus"

	"digilocker_certificate_api/config"
	"digilocker_certificate_api/services"

	"github.com/go-openapi/runtime/middleware"
)

func SetupHandlers(api *operations.DigilockerCertificateAPIAPI) {
	api.HealthGetHeathHandler = health.GetHeathHandlerFunc(healthFunc)
	api.PullCertificatesPostV1DigilockerPullURIRequestHandler = pull_certificates.PostV1DigilockerPullURIRequestHandlerFunc(pullCredentials)
}

func healthFunc(params health.GetHeathParams) middleware.Responder {
	return health.NewGetHeathOK().WithPayload(&models.HealthOKResponse{Status: "UP"})
}

func pullCredentials(params pull_certificates.PostV1DigilockerPullURIRequestParams) middleware.Responder {
	rawData, err := xml.Marshal(params.Body)
	if err != nil {
		log.Errorf("Error in unmarshalling : %v", err)
	}
	digilockerService := services.Init()
	actualHMAC, err := digilockerService.GenerateHMAC(rawData)
	log.Printf("%v", actualHMAC)
	if err != nil {
		return pull_certificates.NewPostV1DigilockerPullURIRequestOK()
	}
	authMacKey := params.HTTPRequest.Header[config.Config.Digilocker.AuthKeyName][0]
	hMacFromRequest, _ := digilockerService.GetHMACFromRequest(authMacKey)
	validRequest := digilockerService.ValidateHMAC(actualHMAC, hMacFromRequest)
	if validRequest {
		certificate, err := digilockerService.GetCertificate(params.Body.DocDetails.DocType,
			params.Body.DocDetails.CertificateID)

		if err != nil {
			return &pull_certificates.PostV1DigilockerPullURIRequestOK{}
		}
		pullURIResponse := digilockerService.GeneratePullURIResponse(*params.Body, certificate)
		return pull_certificates.NewPostV1DigilockerPullURIRequestOK().WithPayload(&pullURIResponse)
	}
	return &pull_certificates.PostV1DigilockerPullURIRequestOK{}
}
