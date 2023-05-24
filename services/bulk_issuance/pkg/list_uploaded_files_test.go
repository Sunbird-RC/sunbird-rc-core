package pkg

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"
	"testing"

	log "github.com/sirupsen/logrus"
)

func (mock *MockService) GetUploadedFiles(_ string, limit *int64, offset *int64) ([]*models.UploadedFileDTO, error) {
	var files []*models.UploadedFileDTO
	var file db.UploadedFile
	file.Filename = "File"
	file.ID = 1
	file.UserID = "123"
	files = append(files, file.ToDTO())
	return files, nil
}

func TestReturnAllFilesForThisUser(t *testing.T) {
	controllers := Controllers{
		&MockService{},
	}
	params := uploaded_files.GetV1UploadsParams{}
	principal := models.JWTClaimBody{
		UserID: "123",
	}
	response := controllers.getUserUploadedFiles(params, &principal)
	log.Infof("%v", response)
}
