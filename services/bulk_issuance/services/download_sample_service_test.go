package services

import (
	"bytes"
	"encoding/csv"
	"github.com/google/go-cmp/cmp"
	"github.com/google/go-cmp/cmp/cmpopts"
	"strings"
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
	schemaPropertiesExpected := []string{"col2", "col1", "col3"}
	sampleValuesExpected := []string{"string", "string", "string"}
	sampleExpected = append(sampleExpected, schemaPropertiesExpected)
	sampleExpected = append(sampleExpected, sampleValuesExpected)
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(sampleExpected)
	actualCsv := actualBytes.String()
	rows := strings.Split(actualCsv, "\n")
	println(rows)
	headers := strings.Split(rows[0], ",")
	sampleValues := strings.Split(rows[1], ",")
	less := func(a, b string) bool { return a < b }
	assert := assert.New(t)
	assert.True(cmp.Diff(schemaPropertiesExpected, headers, cmpopts.SortSlices(less)) == "")
	assert.True(cmp.Diff(sampleValuesExpected, sampleValues, cmpopts.SortSlices(less)) == "")
	assert.Nil(err)
}
