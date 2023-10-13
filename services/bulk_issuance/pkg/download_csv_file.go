package pkg

import (
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/download_file_report"

	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func (c *Controllers) getCSVReportById(params download_file_report.GetV1IDReportParams, principal *models.JWTClaimBody) middleware.Responder {
	log.Infof("Downloading report file with ID : %v", params.ID)
	response := download_file_report.NewGetV1IDReportOK()
	fileName, fileBytes, err := c.services.GetCSVReport(int(params.ID), principal.UserID)
	if err != nil {
		return download_file_report.
			NewGetV1IDReportForbidden().
			WithPayload(&models.ErrorPayload{Message: "User is not allowed to access this file"})
	}
	response.WithContentDisposition("attachment; filename=\"" + *fileName + "\"").WithPayload(fileBytes)
	log.Infof("Downloading file with name : %v", *fileName)
	return response
}
