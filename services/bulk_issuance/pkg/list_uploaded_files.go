package pkg

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"

	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

var getAllFileDataForUserId = db.GetAllFileDataForUserID

func listFiles(params uploaded_files.GetV1UploadParams, principal *models.JWTClaimBody) middleware.Responder {
	log.Info("Compiling a list of all uploaded files")
	response := uploaded_files.GetV1UploadOK{}
	files, err := getAllFileDataForUserId(principal.UserId)
	if err != nil {
		return uploaded_files.NewGetV1UploadNotFound().WithPayload(err.Error())
	}
	response.SetPayload(files)
	return &response
}
