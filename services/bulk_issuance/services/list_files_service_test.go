package services

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
)

func createTempFile() db.FileData {
	rows := [][]string{{
		"row11", "row12", "row13",
	}}
	bytes, _ := json.Marshal(rows)
	fileData := db.FileData{
		Filename: "Temp.csv",
		RowData:  []byte(bytes),
		Headers:  "col1,col2,col3",
		UserID:   "1",
	}
	return fileData
}

func (mockRepo *MockRepository) GetAllFileDataForUserID(userId string) ([]db.FileData, error) {
	fileData := createTempFile()
	return []db.FileData{fileData}, nil
}

func Test_ListFileForUser(t *testing.T) {
	mockService := MockService{
		Services{
			&MockRepository{},
		},
	}
	actualFiles, err := mockService.GetUploadedFiles("1")
	file := createTempFile()
	expectedFiles := []*models.UploadedFiles{file.ToDTO()}
	assert := assert.New(t)
	assert.Equal(expectedFiles, actualFiles)
	assert.Nil(err)
}
