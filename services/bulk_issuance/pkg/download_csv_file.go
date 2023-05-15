package pkg

import (
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/download_file_report"

	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func (c *Controllers) downloadReportFile(params download_file_report.GetV1IDReportParams, principal *models.JWTClaimBody) middleware.Responder {
	log.Infof("Downloading report file with ID : %v", params.ID)
	response := download_file_report.NewGetV1IDReportOK()
	fileName, bytes, err := c.services.DownloadCSVReport(int(params.ID), principal.UserID)
	if err != nil {
		return download_file_report.NewGetV1IDReportForbidden().WithPayload("User is not allowed to access this file")
	}
	if err != nil {
		return download_file_report.NewGetV1IDReportNotFound().WithPayload(err.Error())
	}
	response.WithContentDisposition("attachment; filename=\"" + *fileName + "\"").WithPayload(bytes)
	log.Infof("Downloading file with name : %v", *fileName)
	return response
}
