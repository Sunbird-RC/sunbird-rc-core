package services

import (
	"bulk_issuance/config"
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/utils"
	"encoding/csv"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"sort"
	"strings"
	"time"

	log "github.com/sirupsen/logrus"
)

var insert = db.Insert
var client = &http.Client{}
var read = ioutil.ReadAll

type Scanner struct {
	Reader *csv.Reader
	Head   map[string]int
	Row    []string
}

func InsertIntoFileData(rows [][]string, fileName string, data Scanner, principal *models.JWTClaimBody) (uint, error) {
	log.Info("adding entry to dbFileData")
	bytes, err := json.Marshal(rows)
	utils.LogErrorIfAny("Error while marshalling data for database : %v", err)
	fileUpload := db.FileData{
		Filename:     fileName,
		Headers:      getHeaders(data.Head),
		TotalRecords: len(rows),
		RowData:      bytes,
		UserID:       principal.UserId,
		UserName:     principal.PreferredUsername,
		Date:         time.Now().Format("2006-01-02"),
	}
	return insert(&fileUpload)
}

func (o *Scanner) Scan() bool {
	a, e := o.Reader.Read()
	if e != nil {
		log.Errorf("Parsing error : %v", e)
	}
	o.Row = a
	return e == nil
}

func ProcessDataFromCSV(data *Scanner, header http.Header, vcName string) (int, int, [][]string, error) {
	var (
		totalSuccess int = 0
		totalErrors  int = 0
	)
	rows := make([][]string, 0)
	log.Info("processing all rows from csv")
	properties, err := GetSchemaProperties(vcName)
	if err != nil {
		return 0, 0, rows, err
	}
	for data.Scan() {
		jsonBody := make(map[string]interface{})
		bytes := createReqBody(properties, jsonBody, data)
		res, err := createSingleRecord(vcName, bytes, header)
		utils.LogErrorIfAny("Error in creating a record : %v", err)
		currRow := data.Row
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

func createSingleRecord(vcName string, bytes []byte, header http.Header) (*http.Response, error) {
	methodName := "POST"
	req, err := http.NewRequest(methodName, config.Config.Registry.BaseUrl+"api/v1/"+vcName, strings.NewReader(string(bytes)))
	utils.LogErrorIfAny("Error in creating request %v : %v", err, config.Config.Registry.BaseUrl+"api/v1/"+vcName)
	req.Header.Set("Authorization", header["Authorization"][0])
	req.Header.Set("Content-Type", "application/json")
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

func appendErrorsToCurrentRow(res *http.Response, data *Scanner, lastColIndex int, currRow []string) []string {
	resBody, err := read(res.Body)
	utils.LogErrorIfAny("Error while reading error reponse from adding single record : %v", err)
	data.Head["Errors"] = lastColIndex
	var responseMap map[string]interface{}
	err = json.Unmarshal(resBody, &responseMap)
	if err != nil {
		log.Errorf("Unmarshal Error : %v", err)
	}
	errObj := responseMap["params"].(map[string]interface{})
	currRow = append(currRow, errObj["errmsg"].(string))
	return currRow
}