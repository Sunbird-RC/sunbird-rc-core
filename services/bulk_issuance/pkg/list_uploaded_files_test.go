package pkg

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"
	"testing"

	log "github.com/sirupsen/logrus"
)

func (mock *MockService) ListFileForUser(_ string) ([]db.FileData, error) {
	var files []db.FileData
	var file db.FileData
	file.Filename = "File"
	file.ID = 1
	file.UserID = "123"
	files = append(files, file)
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
	response := controllers.listFiles(params, &principal)
	log.Infof("%v", response)
}
