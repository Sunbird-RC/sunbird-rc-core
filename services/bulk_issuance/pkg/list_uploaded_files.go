package pkg

import (
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"

	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func (c *Controllers) getUserUploadedFiles(params uploaded_files.GetV1UploadsParams, principal *models.JWTClaimBody) middleware.Responder {
	log.Info("Compiling a list of all uploaded files")
	response := uploaded_files.GetV1UploadsOK{}
	files, err := c.services.GetUploadedFiles(principal.UserID, params.Limit, params.Offset)
	if err != nil {
		return uploaded_files.NewGetV1UploadsNotFound().WithPayload(err.Error())
	}
	response.SetPayload(&models.UploadedFilesResponse{
		Files: files,
	})
	return &response
}
