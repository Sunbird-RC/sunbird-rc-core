package services

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"encoding/json"
	"io"
	"net/http"
	"testing"

	newbytes "bytes"

	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

func TestAddEntryForDbFilesToDatabase(t *testing.T) {
	rows := [][]string{
		{"col1", "col2", "col3"}, {"row11", "row12", "row13"},
	}
	old := insert
	defer func() { insert = old }()
	fileName := "temp.csv"
	var data Scanner = Scanner{
		Head: map[string]int{
			"col1": 1,
			"col2": 2,
			"col3": 3,
		},
	}
	principal := models.JWTClaimBody{
		UserId:            "1",
		PreferredUsername: "Temp",
	}
	insert = func(data *db.FileData) (uint, error) {
		return 1, nil
	}
	response, _ := InsertIntoFileData(rows, fileName, data, &principal)
	log.Infof("Response : %v", response)
	expected := uint(1)
	assert := assert.New(t)
	assert.Equal(expected, response)
}

func Test_getHeaders(t *testing.T) {
	head := map[string]int{
		"col1": 1,
		"col2": 2,
		"col3": 3,
	}
	expected := "col1,col2,col3"
	response := getHeaders(head)
	assert := assert.New(t)
	assert.Equal(expected, response)
}

func Test_createReqBody(t *testing.T) {
	properties := []string{"col1", "col2", "col3"}
	jsonBody := map[string]interface{}{}
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
	expectedBytes, _ := json.Marshal(expectedJson)
	assert := assert.New(t)
	assert.Equal(expectedBytes, createReqBody(properties, jsonBody, &data))
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
	header := http.Header{
		"Authorization": []string{"Bearer asd"},
	}
	bytes, _ := json.Marshal(jsonBody)
	response, _ := createSingleRecord(vcName, bytes, header)
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
	old := read
	defer func() {
		read = old
	}()
	b := new(newbytes.Buffer)
	json.NewEncoder(b).Encode(response)
	read = func(r io.Reader) ([]byte, error) {
		return json.Marshal(response)
	}
	res := &http.Response{
		Body: NopCloser(b),
	}
	data := Scanner{
		Head: map[string]int{
			"col1": 0,
			"col2": 1,
			"col3": 2,
		},
	}
	currRow := []string{"row11", "row12", "row13"}
	expectedRow := []string{"row11", "row12", "row13", ""}
	actualRow := appendErrorsToCurrentRow(res, &data, 3, currRow)
	assert := assert.New(t)
	assert.Equal(expectedRow, actualRow)
}
