package pkg

import (
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/sample_template"
	"bulk_issuance/utils"
	"bytes"
	"encoding/csv"

	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func downloadSampleFile(params sample_template.GetV1SchemaNameSampleCsvParams, principal *models.JWTClaimBody) middleware.Responder {
	log.Infof("Downloading sample file with name %v", (params.SchemaName + ".csv"))
	response := sample_template.NewGetV1SchemaNameSampleCsvOK()
	schemaProperties, sampleValues, err := utils.GetSchemaPropertiesAndSampleValues(params.SchemaName)
	if err != nil {
		response := sample_template.NewGetV1SchemaNameSampleCsvNotFound()
		response.SetPayload(err.Error())
		return response
	}
	csvData := make([][]string, 0)
	csvData = append(csvData, schemaProperties)
	csvData = append(csvData, sampleValues)
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(csvData)
	log.Infof("Headers for schema %v : %v", params.SchemaName, csvData)
	utils.LogErrorIfAny("Error while opening file : %v", err)
	response.WithContentDisposition("attachment; filename=\"" + params.SchemaName + ".csv\"").WithPayload(b)
	return response
}
