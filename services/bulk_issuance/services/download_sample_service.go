package services

import (
	"bytes"
	"encoding/csv"

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
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	err = w.WriteAll(csvData)
	if err != nil {
		log.Error("Error while writing data to csv file", err)
		return nil, err
	}
	log.Infof("Headers for schema %v : %v", schemaName, csvData)
	return b, nil
}
