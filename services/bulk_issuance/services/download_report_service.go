package services

import (
	"bulk_issuance/utils"
	"bytes"
	"encoding/csv"
	"encoding/json"
	"strings"
)

func (services *Services) DownloadCSVReport(id int, userId string) (*string, *bytes.Buffer, error) {
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
	w.WriteAll(data)
	return &file.Filename, b, nil
}
