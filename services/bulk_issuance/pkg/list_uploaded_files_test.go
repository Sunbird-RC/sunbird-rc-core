package pkg

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"
	"testing"

	log "github.com/sirupsen/logrus"
)

func TestReturnAllFilesForThisUser(t *testing.T) {
	getAllFileDataForUserId = func(userId string) ([]db.FileData, error) {
		var files []db.FileData
		var file db.FileData
		file.Filename = "File"
		file.ID = 1
		files = append(files, file)
		return files, nil
	}
	params := uploaded_files.GetV1UploadParams{}
	principal := models.JWTClaimBody{
		UserId: "123",
	}
	response := listFiles(params, &principal)
	log.Infof("%v", response)
}
