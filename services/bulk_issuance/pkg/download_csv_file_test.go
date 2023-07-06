package pkg

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/download_file_report"
	"bytes"
	"encoding/csv"
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
)

type MockRepository struct {
	db.Repository
}

func (mock *MockRepository) GetUploadedFileByIdAndUserId(_ int, userId string) (*db.UploadedFile, error) {
	var file db.UploadedFile
	rows := [][]string{{"row11"}, {"row12"}, {"row13"}}
	headers := "col1,col2,col3"
	rowBytes, _ := json.Marshal(rows)
	file.Filename = "Temp.csv"
	file.RowData = rowBytes
	file.Headers = headers
	file.UserID = "1"
	return &file, nil
}

func (mock *MockService) GetCSVReport(id int, userId string) (*string, *bytes.Buffer, error) {
	data := [][]string{{"col1", "col2", "col3"}, {"row11"}, {"row12"}, {"row13"}}
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(data)
	fileName := "Temp.csv"
	return &fileName, b, nil
}

func Test_downloadReportFile(t *testing.T) {
	controllers := Controllers{
		&MockService{},
	}
	params := download_file_report.GetV1IDReportParams{ID: 1}
	principal := models.JWTClaimBody{UserID: "1"}
	actualResponse := controllers.getCSVReportById(params, &principal)
	expectedResponse := download_file_report.NewGetV1IDReportOK()
	data := [][]string{{"col1", "col2", "col3"}, {"row11"}, {"row12"}, {"row13"}}
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(data)
	expectedResponse.WithContentDisposition("attachment; filename=\"Temp.csv\"").WithPayload(b)
	assert := assert.New(t)
	assert.Equal(actualResponse, expectedResponse)
}
