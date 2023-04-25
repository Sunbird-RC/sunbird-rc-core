package pkg

import (
	"bulk_issuance/swagger_gen/restapi/operations/sample_template"
	"bytes"
	"encoding/csv"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_sampleCSV(t *testing.T) {
	old := getSchemaProperties
	defer func() { getSchemaProperties = old }()
	getSchemaProperties = func(key string) ([]string, []string, error) {
		return []string{"col1", "col2", "col3"}, []string{"row11", "row12", "row13"}, nil
	}
	params := sample_template.GetV1SchemaNameSampleCsvParams{SchemaName: "Temp"}
	actualResp := downloadSampleFile(params, nil)
	expectedResp := &sample_template.GetV1SchemaNameSampleCsvOK{}
	csvData := make([][]string, 0)
	csvData = append(csvData, []string{"col1", "col2", "col3"})
	csvData = append(csvData, []string{"row11", "row12", "row13"})
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(csvData)
	expectedResp.WithContentDisposition("attachment; filename=\"" + params.SchemaName + ".csv\"").WithPayload(b)
	assert := assert.New(t)
	assert.Equal(actualResp, expectedResp)
}
