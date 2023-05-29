package pkg

import (
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/upload_and_create_records"
	"bulk_issuance/utils"
	"io"
	"strings"

	"github.com/Clever/csvlint"
	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func (c *Controllers) createRecordsForSchema(params upload_and_create_records.PostV1SchemaNameUploadParams,
	principal *models.JWTClaimBody) middleware.Responder {
	log.Info("Creating records")
	fileBytes, err := io.ReadAll(params.File)
	if err != nil {
		upload_and_create_records.NewPostV1SchemaNameUploadBadRequest().
			WithPayload(&models.ErrorPayload{
				Message: "Error reading request file",
			})
	}
	csvError, _, _ := csvlint.Validate(strings.NewReader(string(fileBytes)), ',', false)
	if len(csvError) != 0 {
		return upload_and_create_records.NewPostV1SchemaNameUploadBadRequest().
			WithPayload(&models.ErrorPayload{
				Message: "Invalid CSV File",
			})
	}
	totalSuccess, totalErrors, rows, header, err := c.services.
		ProcessDataFromCSV(params.HTTPRequest.Header, params.SchemaName, strings.NewReader(string(fileBytes)))

	if err != nil {
		return upload_and_create_records.NewPostV1SchemaNameUploadNotFound().WithPayload(&models.ErrorPayload{
			Message: err.Error(),
		})
	}

	fileName := getFileName(params)
	id, err := c.services.InsertIntoFileData(rows, fileName, header, principal)
	utils.LogErrorIfAny("Error while adding entry to table FileData : %v", err)
	successFailureCount := map[string]uint{
		"success":   uint(totalSuccess),
		"error":     uint(totalErrors),
		"totalRows": uint(totalSuccess + totalErrors),
		"ID":        id,
	}
	response := upload_and_create_records.NewPostV1SchemaNameUploadOK().WithPayload(successFailureCount)
	return response
}

func getFileName(params upload_and_create_records.PostV1SchemaNameUploadParams) string {
	_, fileHeader, err := params.HTTPRequest.FormFile("file")
	utils.LogErrorIfAny("Error retrieving file from request : %v", err)
	return fileHeader.Filename
}
