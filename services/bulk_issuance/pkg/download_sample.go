package pkg

import (
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/sample_template"

	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func (controller *Controllers) downloadSampleFile(params sample_template.GetV1SchemaNameSampleCsvParams, _ *models.JWTClaimBody) middleware.Responder {
	log.Infof("Downloading sample file with name %v", (params.SchemaName + ".csv"))
	response := sample_template.NewGetV1SchemaNameSampleCsvOK()
	sampleCSVBytes, err := controller.services.GetSampleCSVForSchema(params.SchemaName)
	if err != nil {
		response := sample_template.NewGetV1SchemaNameSampleCsvNotFound()
		response.SetPayload(err.Error())
		return response
	}
	response.WithContentDisposition("attachment; filename=\"" + params.SchemaName + ".csv\"").WithPayload(sampleCSVBytes)
	return response
}
