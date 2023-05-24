package services

import (
	"bulk_issuance/config"
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/utils"
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"time"

	log "github.com/sirupsen/logrus"
)

var client = &http.Client{}

type Services struct {
	repo db.IRepo
}

type IService interface {
	GetSampleCSVForSchema(schemaName string) (*bytes.Buffer, error)
	InsertIntoFileData(rows [][]string, fileName string, header string, principal *models.JWTClaimBody) (uint, error)
	ProcessDataFromCSV(header http.Header, vcName string, file io.Reader) (int, int, [][]string, string, error)
	GetCSVReport(id int, userId string) (*string, *bytes.Buffer, error)
	GetUploadedFiles(userId string, limit *int64, offset *int64) ([]*models.UploadedFileDTO, error)
}

func Init(repository db.IRepo) IService {
	services := Services{
		repo: repository,
	}
	return &services
}

func (services *Services) InsertIntoFileData(rows [][]string, fileName string, header string, principal *models.JWTClaimBody) (uint, error) {
	log.Info("adding entry to dbFileData")
	rowBytes, err := json.Marshal(rows)
	utils.LogErrorIfAny("Error while marshalling data for database : %v", err)
	fileUpload := db.UploadedFile{
		Filename:     fileName,
		Headers:      header,
		TotalRecords: len(rows),
		RowData:      rowBytes,
		UserID:       principal.UserID,
		UserName:     principal.PreferredUsername,
		Date:         time.Now().Format("2006-01-02"),
	}

	return services.repo.Insert(&fileUpload)
}

func (services *Services) ProcessDataFromCSV(header http.Header, schemaName string, file io.Reader) (int, int, [][]string, string, error) {
	csvScanner, err := NewScanner(file)
	if err != nil {
		return 0, 0, nil, "", err
	}
	var (
		totalSuccess = 0
		totalErrors  = 0
	)
	rows := make([][]string, 0)
	log.Info("processing all rows from csv")
	properties, err := getSchemaPropertyNames(schemaName)
	if err != nil {
		return 0, 0, rows, "", err
	}
	for csvScanner.Scan() {
		currRow := csvScanner.Row
		schemaRequest := createSchemaRequest(properties, currRow, csvScanner.Head)
		res, err := callRegistryAPI(schemaName, schemaRequest, header["Authorization"][0])
		utils.LogErrorIfAny("Error in creating a record : %v", err)
		if res.StatusCode != 200 {
			csvScanner.appendHeader("Errors")
			currRow = appendErrorsToCurrentRow(res, currRow)
			totalErrors += 1
		} else {
			totalSuccess += 1
		}
		rows = append(rows, currRow)
	}
	log.Info("processed all rows from csv")
	return totalSuccess, totalErrors, rows, csvScanner.getHeaderAsString(), nil
}

func callRegistryAPI(schemaName string, schemaRequest map[string]interface{}, token string) (*http.Response, error) {
	methodName := "POST"
	postBody, err := json.Marshal(schemaRequest)
	utils.LogErrorIfAny("Error in creating request %v : %v", err, config.Config.Registry.BaseUrl+"api/v1/"+schemaName)
	req, err := http.NewRequest(methodName, config.Config.Registry.BaseUrl+"api/v1/"+schemaName, bytes.NewBuffer(postBody))
	utils.LogErrorIfAny("Error in creating request %v : %v", err, config.Config.Registry.BaseUrl+"api/v1/"+schemaName)
	req.Header.Set("Authorization", token)
	req.Header.Set("Content-Type", "application/json")
	return client.Do(req)
}

func createSchemaRequest(properties []string, row []string, head map[string]int) map[string]interface{} {
	jsonBody := make(map[string]interface{})
	for _, property := range properties {
		jsonBody[property] = row[head[property]]
	}
	return jsonBody
}

func appendErrorsToCurrentRow(res *http.Response, currRow []string) []string {
	resBody, err := io.ReadAll(res.Body)
	utils.LogErrorIfAny("Error while reading error response from adding single record : %v", err)
	var responseMap map[string]interface{}
	err = json.Unmarshal(resBody, &responseMap)
	if err != nil {
		log.Errorf("Unmarshal Error : %v", err)
	}
	errObj := responseMap["params"].(map[string]interface{})
	currRow = append(currRow, errObj["errmsg"].(string))
	return currRow
}
