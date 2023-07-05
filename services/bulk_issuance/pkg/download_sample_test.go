package pkg

import (
	"bulk_issuance/services"
	"bulk_issuance/swagger_gen/restapi/operations/sample_template"
	"bytes"
	"encoding/csv"
	"testing"

	"github.com/stretchr/testify/assert"
)

type MockService struct {
	services.IService
}

func (mockService *MockService) GetSampleCSVForSchema(schemaName string) (*bytes.Buffer, error) {
	csvData := make([][]string, 0)
	csvData = append(csvData, []string{"col1", "col2", "col3"})
	csvData = append(csvData, []string{"row11", "row12", "row13"})
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(csvData)
	return b, nil
}

func Test_sampleCSV(t *testing.T) {
	controllers := Controllers{
		&MockService{},
	}
	params := sample_template.GetV1SchemaNameSampleCsvParams{SchemaName: "Temp"}
	actualResp := controllers.getSampleCSVForSchema(params, nil)
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
