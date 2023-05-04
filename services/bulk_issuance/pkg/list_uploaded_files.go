package pkg

import (
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"

	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func (controllers *Controllers) listFiles(params uploaded_files.GetV1UploadParams, principal *models.JWTClaimBody) middleware.Responder {
	log.Info("Compiling a list of all uploaded files")
	response := uploaded_files.GetV1UploadOK{}
	files, err := controllers.services.ListFileForUser(principal.UserId)
	if err != nil {
		return uploaded_files.NewGetV1UploadNotFound().WithPayload(err.Error())
	}
	response.SetPayload(files)
	return &response
}
