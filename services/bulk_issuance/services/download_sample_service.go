package services

import (
	"bytes"
	"encoding/csv"

	log "github.com/sirupsen/logrus"
)

func (services *Services) GetSampleCSVForSchema(schemaName string) (*bytes.Buffer, error) {
	schemaProperties, sampleValues, err := services.GetSchemaPropertiesAndSampleValues(schemaName)
	if err != nil {
		return nil, err
	}
	csvData := make([][]string, 0)
	csvData = append(csvData, schemaProperties)
	csvData = append(csvData, sampleValues)
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(csvData)
	log.Infof("Headers for schema %v : %v", schemaName, csvData)
	return b, nil
}
