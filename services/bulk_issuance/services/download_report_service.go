package services

import (
	"bulk_issuance/utils"
	"bytes"
	"encoding/csv"
	"encoding/json"
	log "github.com/sirupsen/logrus"
	"strings"
)

func (services *Services) GetCSVReport(id int, userId string) (*string, *bytes.Buffer, error) {
	file, err := services.repo.GetFileDataByIdAndUser(id, userId)
	if err != nil {
		return nil, nil, err
	}
	var data [][]string
	err = json.Unmarshal(file.RowData, &data)
	utils.LogErrorIfAny("Error while unmarshalling row data for downloading report of file : %v ", err)
	data = append([][]string{strings.Split(file.Headers, ",")}, data...)
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	err = w.WriteAll(data)
	if err != nil {
		log.Error("Error while writing data to csv, ", err)
		return nil, nil, err
	}
	return &file.Filename, b, nil
}
