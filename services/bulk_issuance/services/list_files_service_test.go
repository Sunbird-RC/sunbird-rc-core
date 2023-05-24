package services

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
)

func createTempFile() db.UploadedFile {
	rows := [][]string{{
		"row11", "row12", "row13",
	}}
	bytes, _ := json.Marshal(rows)
	fileData := db.UploadedFile{
		Filename: "Temp.csv",
		RowData:  []byte(bytes),
		Headers:  "col1,col2,col3",
		UserID:   "1",
	}
	return fileData
}

func (mockRepo *MockRepository) GetAllUploadedFilesByUserId(userId string, pagination db.Pagination) ([]db.UploadedFile, error) {
	fileData := createTempFile()
	return []db.UploadedFile{fileData}, nil
}

func Test_ListFileForUser(t *testing.T) {
	mockService := MockService{
		Services{
			&MockRepository{},
		},
	}
	var defaultValue int64 = 0
	actualFiles, err := mockService.GetUploadedFiles("1", &defaultValue, &defaultValue)
	file := createTempFile()
	expectedFiles := []*models.UploadedFileDTO{file.ToDTO()}
	assert := assert.New(t)
	assert.Equal(expectedFiles, actualFiles)
	assert.Nil(err)
}
