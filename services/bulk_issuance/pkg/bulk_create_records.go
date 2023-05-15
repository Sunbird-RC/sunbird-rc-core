package pkg

import (
	"bulk_issuance/services"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/upload_and_create_records"
	"bulk_issuance/utils"
	"encoding/csv"
	"io"
	"strings"

	"github.com/Clever/csvlint"
	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func NewScanner(o io.Reader) (services.Scanner, error) {
	csv_o := csv.NewReader(o)
	header, e := csv_o.Read()
	if e != nil {
		log.Errorf("Parsing error : %v", e)
		return services.Scanner{}, e
	}
	m := map[string]int{}
	for n, s := range header {
		m[strings.TrimSpace(s)] = n
	}
	return services.Scanner{Reader: csv_o, Head: m}, nil
}

func (controller *Controllers) createRecords(params upload_and_create_records.PostV1SchemaNameUploadParams, principal *models.JWTClaimBody) middleware.Responder {
	log.Info("Creating records")
	data, err := NewScanner(params.File)
	csvError, _, _ := csvlint.Validate(params.File, ',', false)
	if data.Reader == nil || err != nil || len(csvError) != 0 {
		return upload_and_create_records.NewPostV1SchemaNameUploadBadRequest().
			WithPayload(&upload_and_create_records.PostV1SchemaNameUploadBadRequestBody{
				Message: "Invalid CSV File",
			})
	}
	totalSuccess, totalErrors, rows, err := controllers.services.ProcessDataFromCSV(&data, params.HTTPRequest.Header, params.SchemaName)
	if err != nil {
		return upload_and_create_records.NewPostV1SchemaNameUploadNotFound().WithPayload(err.Error())
	}
	response := upload_and_create_records.NewPostV1SchemaNameUploadOK()
	_, fileHeader, err := params.HTTPRequest.FormFile("file")
	utils.LogErrorIfAny("Error retrieving file from request : %v", err)
	fileName := fileHeader.Filename
	id, err := controllers.services.InsertIntoFileData(rows, fileName, data, principal)
	successFailureCount := map[string]uint{
		"success":   uint(totalSuccess),
		"error":     uint(totalErrors),
		"totalRows": uint(totalSuccess + totalErrors),
		"ID":        id,
	}
	response.SetPayload(successFailureCount)
	utils.LogErrorIfAny("Error while adding entry to table FileData : %v", err)
	return response
}
