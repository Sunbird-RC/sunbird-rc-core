package pkg

import (
	"bulk_issuance/config"
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/upload_and_create_records"
	"bulk_issuance/utils"
	"encoding/csv"
	"encoding/json"
	"io"
	"io/ioutil"
	"net/http"
	"sort"
	"strings"
	"time"

	"github.com/Clever/csvlint"
	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

type Scanner struct {
	Reader *csv.Reader
	Head   map[string]int
	Row    []string
}

func NewScanner(o io.Reader) (Scanner, error) {
	csv_o := csv.NewReader(o)
	header, e := csv_o.Read()
	if e != nil {
		log.Errorf("Parsing error : %v", e)
		return Scanner{}, e
	}
	m := map[string]int{}
	for n, s := range header {
		m[strings.TrimSpace(s)] = n
	}
	return Scanner{Reader: csv_o, Head: m}, nil
}

func (o *Scanner) Scan() bool {
	a, e := o.Reader.Read()
	if e != nil {
		log.Errorf("Parsing error : %v", e)
	}
	o.Row = a
	return e == nil
}

func createRecords(params upload_and_create_records.PostV1UploadFilesVCNameParams, principal *models.JWTClaimBody) middleware.Responder {
	log.Info("Creating records")
	data, err := NewScanner(params.File)
	csvError, _, _ := csvlint.Validate(params.File, ',', false)
	if data.Reader == nil || err != nil || len(csvError) != 0 {
		return upload_and_create_records.NewPostV1UploadFilesVCNameInternalServerError().WithPayload("Invalid CSV File")
	}
	totalSuccess, totalErrors, rows, err := processDataFromCSV(&data, params.HTTPRequest.Header, params.VCName)
	if err != nil {
		return upload_and_create_records.NewPostV1UploadFilesVCNameNotFound().WithPayload(err.Error())
	}
	successFailureCount := map[string]int{
		"success":   totalSuccess,
		"error":     totalErrors,
		"totalRows": totalSuccess + totalErrors,
	}
	response := upload_and_create_records.NewPostV1UploadFilesVCNameOK()
	response.SetPayload(successFailureCount)
	_, fileHeader, err := params.HTTPRequest.FormFile("file")
	utils.LogErrorIfAny("Error retrieving file from request : %v", err)
	fileName := fileHeader.Filename
	err = addEntryForDbUploadToDatabase(fileName, totalSuccess, totalErrors, principal)
	utils.LogErrorIfAny("Error while adding entry to table DBFiles : %v", err)
	err = addEntryForDbFilesToDatabase(rows, fileName, data)
	utils.LogErrorIfAny("Error while adding entry to table DBFileData : %v", err)
	return response
}

func addEntryForDbFilesToDatabase(rows [][]string, fileName string, data Scanner) error {
	log.Info("adding entry to dbFileData")
	bytes, err := json.Marshal(rows)
	utils.LogErrorIfAny("Error while marshalling data for database : %v", err)
	fileUpload := db.DBFileData{
		Filename: fileName,
		Headers:  getHeaders(data.Head),
		RowData:  bytes,
	}
	return db.CreateDBFileData(&fileUpload)
}

func addEntryForDbUploadToDatabase(fileName string, totalSuccess int, totalErrors int, principal *models.JWTClaimBody) error {
	log.Info("adding entry to dbFiles")
	dbUpload := db.DBFiles{
		Filename:     fileName,
		TotalRecords: totalSuccess + totalErrors,
		UserID:       principal.PreferredUsername,
		Date:         time.Now().Format("2006-01-02"),
	}
	return db.CreateDBFiles(&dbUpload)
}

func processDataFromCSV(data *Scanner, header http.Header, vcName string) (int, int, [][]string, error) {
	var (
		totalSuccess int = 0
		totalErrors  int = 0
	)
	rows := make([][]string, 0)
	log.Info("processing all rows from csv")
	properties, err := utils.GetSchemaProperties(vcName)
	if err != nil {
		return 0, 0, rows, err
	}
	for data.Scan() {
		jsonBody := make(map[string]interface{})
		bytes := createReqBody(properties, jsonBody, data)
		res, err := createSingleRecord(vcName, bytes, header)
		utils.LogErrorIfAny("Error in creating a record : %v", err)
		currRow := make([]string, 0)
		currRow = data.Row
		if res.StatusCode != 200 {
			lastColIndex := len(properties) + 1
			currRow = appendErrorsToCurrentRow(res, data, lastColIndex, currRow)
			totalErrors += 1
		} else {
			totalSuccess += 1
		}
		rows = append(rows, currRow)
	}
	log.Info("processed all rows from csv")
	return totalSuccess, totalErrors, rows, nil
}

func appendErrorsToCurrentRow(res *http.Response, data *Scanner, lastColIndex int, currRow []string) []string {
	resBody, err := ioutil.ReadAll(res.Body)
	utils.LogErrorIfAny("Error while reading error reponse from adding single record : %v", err)
	data.Head["Errors"] = lastColIndex
	currRow = append(currRow, string(resBody))
	return currRow
}

func createSingleRecord(vcName string, bytes []byte, header http.Header) (*http.Response, error) {
	methodName := "POST"
	req, err := http.NewRequest(methodName, config.Config.Registry.BaseUrl+"api/v1/"+vcName, strings.NewReader(string(bytes)))
	utils.LogErrorIfAny("Error in creating request %v : %v", err, config.Config.Registry.BaseUrl+"api/v1/"+vcName)
	req.Header.Set("Authorization", header["Authorization"][0])
	req.Header.Set("Content-Type", "application/json")
	client := &http.Client{}
	return client.Do(req)
}

func createReqBody(properties []string, jsonBody map[string]interface{}, data *Scanner) []byte {
	for _, k := range properties {
		jsonBody[k] = data.Row[data.Head[k]]
	}
	bytes, err := json.Marshal(jsonBody)
	utils.LogErrorIfAny("Error while marshalling data for creating req body : %v", err)
	return bytes
}

func getHeaders(head map[string]int) string {
	headers := make([]string, 0)
	for k := range head {
		headers = append(headers, k)
	}
	sort.SliceStable(headers, func(i, j int) bool {
		return head[headers[i]] < head[headers[j]]
	})
	return strings.Join(headers, ",")
}
