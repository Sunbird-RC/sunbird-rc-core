package pkg

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"

	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func listFiles(params uploaded_files.GetV1UploadedFilesParams, principal *models.JWTClaimBody) middleware.Responder {
	log.Info("Compiling a list of all uploaded files")
	response := uploaded_files.GetV1BulkUploadedFilesOK{}
	files, err := db.GetAllUploadedFilesData()
	if err != nil {
		return uploaded_files.NewGetV1BulkUploadedFilesNotFound().WithPayload(err.Error())
	}
	response.SetPayload(files)
	return &response
}
