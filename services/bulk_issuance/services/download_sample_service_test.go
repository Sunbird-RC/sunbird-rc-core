package services

import (
	"bytes"
	"encoding/csv"
	"testing"

	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/assert"
)

func Test_GetSampleCSVForSchema(t *testing.T) {
	mockService := MockService{
		Services{
			&MockRepository{},
		},
	}
	definitions := map[string]interface{}{
		"definitions": map[string]interface{}{
			"Temp": map[string]interface{}{
				"properties": map[string]interface{}{
					"col1": map[string]interface{}{
						"type": "string",
					},
					"col2": map[string]interface{}{
						"type": "string",
					},
					"col3": map[string]interface{}{
						"type": "string",
					},
				},
			},
		},
	}
	httpmock.Activate()
	jsonResponder, _ := httpmock.NewJsonResponder(200, definitions)
	httpmock.RegisterResponder("GET", "api/docs/swagger.json",
		jsonResponder)
	actualBytes, err := mockService.GetSampleCSVForSchema("Temp")
	sampleExpected := make([][]string, 0)
	schemaProperties := []string{"col1", "col2", "col3"}
	sampleValues := []string{"string", "string", "string"}
	sampleExpected = append(sampleExpected, schemaProperties)
	sampleExpected = append(sampleExpected, sampleValues)
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(sampleExpected)
	assert := assert.New(t)
	assert.Equal(b, actualBytes)
	assert.Nil(err)
}
