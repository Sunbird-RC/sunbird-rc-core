package services

import (
	"bulk_issuance/utils"
	"bytes"
	log "github.com/sirupsen/logrus"
)

func (services *Services) GetSampleCSVForSchema(schemaName string) (*bytes.Buffer, error) {
	schemaProperties, sampleValues, err := getSchemaPropertiesAndSampleValues(schemaName)
	if err != nil {
		return nil, err
	}
	csvData := make([][]string, 0)
	csvData = append(csvData, schemaProperties)
	csvData = append(csvData, sampleValues)
	csvBuffer, err := utils.CreateCSVBuffer(csvData)
	if err != nil {
		return nil, err
	}
	log.Debugf("Headers for schema %v : %v", schemaName, csvData)
	return csvBuffer, nil
}
