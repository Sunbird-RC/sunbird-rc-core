package services

import (
	"bulk_issuance/db"
	"bytes"
	"encoding/csv"
	"encoding/json"

	"testing"

	"github.com/stretchr/testify/assert"
)

func (mockRepo *MockRepository) GetUploadedFileByIdAndUserId(id int, userId string) (*db.UploadedFile, error) {
	rows := [][]string{{
		"row11", "row12", "row13",
	}}
	bytes, _ := json.Marshal(rows)
	file := db.UploadedFile{
		Filename: "Temp.csv",
		RowData:  []byte(bytes),
		Headers:  "col1,col2,col3",
	}
	return &file, nil
}

func Test_DownloadCSVReport(t *testing.T) {
	mockService := MockService{
		Services{
			&MockRepository{},
		},
	}
	actualFileName, actualBytes, err := mockService.GetCSVReport(1, "2")
	fileWithHeaders := [][]string{
		{"col1", "col2", "col3"},
		{"row11", "row12", "row13"},
	}
	expectedFileName := "Temp.csv"
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(fileWithHeaders)
	assert := assert.New(t)
	assert.Equal(*actualFileName, expectedFileName)
	assert.Equal(b, actualBytes)
	assert.Equal(err, nil)
}
