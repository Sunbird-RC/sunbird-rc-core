package pkg

import (
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/upload_and_create_records"
	"io"
	"mime/multipart"
	"net/http"
	"strings"
	"testing"

	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

func (mock *MockService) ProcessDataFromCSV(header http.Header, schemaName string, reader io.Reader) (int, int, [][]string, string, error) {
	data := [][]string{{"col1", "col2", "col3"}, {"row11"}, {"row12"}, {"row13"}}
	return 1, 0, data, "col1,col2,col3", nil
}

func (mock *MockService) InsertIntoFileData(rows [][]string, fileName string, header string, principal *models.JWTClaimBody) (uint, error) {
	log.Info("1234")
	return 1, nil
}
func Test_createRecords(t *testing.T) {
	expectedResponseMap := map[string]uint{
		"success":   uint(1),
		"error":     uint(0),
		"totalRows": uint(1),
		"ID":        1,
	}
	expectedResponse := upload_and_create_records.NewPostV1SchemaNameUploadOK().WithPayload(expectedResponseMap)
	controllers := Controllers{
		&MockService{},
	}
	headers := multipart.FileHeader{
		Filename: "Dummy.csv",
	}
	params := upload_and_create_records.PostV1SchemaNameUploadParams{
		File: io.NopCloser(strings.NewReader("Hello,world")),
		HTTPRequest: &http.Request{
			Header: make(http.Header),
			MultipartForm: &multipart.Form{
				Value: nil,
				File: map[string][]*multipart.FileHeader{
					"file": {&headers},
				},
			},
		},
		SchemaName: "dummySchema",
	}
	actualResponse := controllers.createRecordsForSchema(params, nil)
	assert := assert.New(t)
	assert.Equal(expectedResponse, actualResponse)
}
