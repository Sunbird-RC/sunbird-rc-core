package services

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"encoding/json"
	"io"
	"net/http"
	"strings"
	"testing"

	newbytes "bytes"

	"github.com/jarcoal/httpmock"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

type MockRepository struct {
	db.Repository
}

func (mock *MockRepository) Insert(data *db.UploadedFile) (uint, error) {
	return 1, nil
}

type MockService struct {
	Services
}

func Test_AddEntryForDbFilesToDatabase(t *testing.T) {
	rows := [][]string{
		{"col1", "col2", "col3"}, {"row11", "row12", "row13"},
	}
	mockService := MockService{
		Services{
			&MockRepository{},
		},
	}
	fileName := "temp.csv"

	principal := models.JWTClaimBody{
		UserID:            "1",
		PreferredUsername: "Temp",
	}
	response, _ := mockService.InsertIntoFileData(rows, fileName, "col1,col2,col3", &principal)
	log.Infof("Response : %v", response)
	expected := uint(1)
	assert := assert.New(t)
	assert.Equal(expected, response)
}

func Test_createReqBody(t *testing.T) {
	properties := []string{"col1", "col2", "col3"}
	data := Scanner{
		Head: map[string]int{
			"col1": 0,
			"col2": 1,
			"col3": 2,
		},
		Row: []string{"row11", "row12", "row13"},
	}
	expectedJson := map[string]interface{}{
		"col1": "row11",
		"col2": "row12",
		"col3": "row13",
	}
	assert := assert.New(t)
	assert.Equal(expectedJson, createSchemaRequest(properties, data.Row, data.Head))
}

type RoundTripFunc func(req *http.Request) *http.Response

func (f RoundTripFunc) RoundTrip(req *http.Request) (*http.Response, error) {
	return f(req), nil
}

func Test_createSingleRecord(t *testing.T) {
	old := client
	defer func() { client = old }()
	client = &http.Client{Transport: RoundTripFunc(func(req *http.Request) *http.Response {
		return &http.Response{
			StatusCode: 200,
		}
	})}
	vcName := "Temp"
	jsonBody := map[string]interface{}{
		"col1": "row11",
		"col2": "row12",
		"col3": "row13",
	}

	response, _ := callRegistryAPI(vcName, jsonBody, "Bearer asd")
	assert := assert.New(t)
	assert.Equal(response.StatusCode, 200)
}

type nopCloser struct {
	io.Reader
}

func (nopCloser) Close() error { return nil }

func NopCloser(r io.Reader) io.ReadCloser {
	return nopCloser{r}
}

func Test_appendErrorsToCurrentRow(t *testing.T) {
	response := map[string]map[string]interface{}{
		"params": {
			"errmsg": "",
		},
	}
	b := new(newbytes.Buffer)
	json.NewEncoder(b).Encode(response)
	res := &http.Response{
		Body: NopCloser(b),
	}

	currRow := []string{"row11", "row12", "row13"}
	expectedRow := []string{"row11", "row12", "row13", ""}
	actualRow := appendErrorsToCurrentRow(res, currRow)
	assert := assert.New(t)
	assert.Equal(expectedRow, actualRow)
}

func Test_ProcessDataFromCSVForSuccess(t *testing.T) {
	file := io.NopCloser(strings.NewReader("col1,col2\nrow11,row12"))
	header := http.Header{
		"Authorization": []string{"Bearer abc"},
	}
	definitions := map[string]interface{}{
		"definitions": map[string]interface{}{
			"Temp": map[string]interface{}{
				"properties": map[string]interface{}{
					"col1": map[string]interface{}{
						"type": "string",
					},
					"col2": map[string]interface{}{
						"type": "string",
					},
				},
			},
		},
	}
	httpmock.Activate()
	jsonResponder, _ := httpmock.NewJsonResponder(200, definitions)
	httpmock.RegisterResponder("GET", "api/docs/swagger.json",
		jsonResponder)
	success := map[string]interface{}{
		"id":  "sunbird-rc.registry.create",
		"ver": "1.0",
		"ets": 1684827230790,
		"params": map[string]interface{}{
			"resmsgid": "",
			"msgid":    "b7af768b-f636-4877-9c98-2dbbfce4eead",
			"err":      "",
			"status":   "SUCCESSFUL",
			"errmsg":   "",
		},
		"responseCode": "OK",
		"result": map[string]interface{}{
			"Temp": map[string]interface{}{
				"osid": "1-be03d210-db98-45ac-a3a8-95f45e826f0d",
			},
		},
	}
	jsonResponder, _ = httpmock.NewJsonResponder(200, success)
	httpmock.RegisterResponder("POST", "api/v1/Temp", jsonResponder)
	schemaName := "Temp"
	services := Services{}
	totalSuccess, totalErrors, rows, headers, error := services.ProcessDataFromCSV(header, schemaName, file)
	assert := assert.New(t)
	assert.Equal(1, totalSuccess)
	assert.Equal(0, totalErrors)
	assert.Equal(nil, error)
	assert.Equal([][]string{{"row11", "row12"}}, rows)
	assert.Equal("col1,col2", headers)
}

func Test_ProcessDataFromCSVForError(t *testing.T) {
	file := io.NopCloser(strings.NewReader("col1,col2\nrow11,row12"))
	header := http.Header{
		"Authorization": []string{"Bearer abc"},
	}
	definitions := map[string]interface{}{
		"definitions": map[string]interface{}{
			"Temp": map[string]interface{}{
				"properties": map[string]interface{}{
					"col1": map[string]interface{}{
						"type": "string",
					},
					"col2": map[string]interface{}{
						"type": "string",
					},
				},
			},
		},
	}
	httpmock.Activate()
	jsonResponder, _ := httpmock.NewJsonResponder(200, definitions)
	httpmock.RegisterResponder("GET", "api/docs/swagger.json",
		jsonResponder)
	failure := map[string]interface{}{
		"id":  "sunbird-rc.registry.create",
		"ver": "1.0",
		"ets": 1684827230790,
		"params": map[string]interface{}{
			"resmsgid": "",
			"msgid":    "b7af768b-f636-4877-9c98-2dbbfce4eead",
			"err":      "",
			"status":   "UNSUCCESSFUL",
			"errmsg":   "Some Error Happened",
		},
	}
	jsonResponder, _ = httpmock.NewJsonResponder(400, failure)
	httpmock.RegisterResponder("POST", "api/v1/Temp", jsonResponder)
	schemaName := "Temp"
	services := Services{}
	totalSuccess, totalErrors, rows, headers, error := services.ProcessDataFromCSV(header, schemaName, file)
	assert := assert.New(t)
	assert.Equal(0, totalSuccess)
	assert.Equal(1, totalErrors)
	assert.Equal(nil, error)
	assert.Equal([][]string{{"row11", "row12", "Some Error Happened"}}, rows)
	assert.Equal("col1,col2,Errors", headers)
}
