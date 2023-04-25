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

func Test_downloadReportFile(t *testing.T) {
	old := getFileByIdAndUser
	defer func() { getFileByIdAndUser = old }()
	rows := [][]string{{"row11"}, {"row12"}, {"row13"}}
	headers := "col1,col2,col3"
	rowBytes, _ := json.Marshal(rows)
	getFileByIdAndUser = func(id int, userId string) (*db.FileData, error) {
		var file db.FileData
		file.Filename = "Temp.csv"
		file.RowData = rowBytes
		file.Headers = headers
		file.UserID = "1"
		return &file, nil
	}
	params := download_file_report.GetV1IDReportParams{ID: 1}
	principal := models.JWTClaimBody{UserId: "1"}
	actualResponse := downloadReportFile(params, &principal)
	expectedResponse := download_file_report.NewGetV1IDReportOK()
	data := [][]string{{"col1", "col2", "col3"}, {"row11"}, {"row12"}, {"row13"}}
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(data)
	expectedResponse.WithContentDisposition("attachment; filename=\"Temp.csv\"").WithPayload(b)
	assert := assert.New(t)
	assert.Equal(actualResponse, expectedResponse)
}
