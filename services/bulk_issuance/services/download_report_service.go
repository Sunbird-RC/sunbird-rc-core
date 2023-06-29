package services

import (
	"bulk_issuance/utils"
	"bytes"
	"encoding/json"
	"strings"
)

func (services *Services) GetCSVReport(id int, userId string) (*string, *bytes.Buffer, error) {
	file, err := services.repo.GetUploadedFileByIdAndUserId(id, userId)
	if err != nil {
		return nil, nil, err
	}
	var data [][]string
	err = json.Unmarshal(file.RowData, &data)
	utils.LogErrorIfAny("Error while unmarshalling row data for downloading report of file : %v ", err)
	data = append([][]string{strings.Split(file.Headers, ",")}, data...)
	csvBuffer, err := utils.CreateCSVBuffer(data)
	if err != nil {
		return nil, nil, err
	}
	return &file.Filename, csvBuffer, nil
}
