package services

import (
	"bulk_issuance/config"
	"bulk_issuance/utils"
	"encoding/json"
	"errors"
	"github.com/go-openapi/spec"
	"io"
	"net/http"
	"sort"
)

func getSchemaPropertyNames(schemaName string) ([]string, error) {
	schemaProperties, err := getSchemaProperties(schemaName)
	if err == nil {
		properties := make([]string, 0)
		for k := range schemaProperties {
			properties = append(properties, k)
		}
		sort.Strings(properties)
		return properties, nil
	} else {
		return nil, err
	}
}

func getSchemaPropertiesAndSampleValues(schemaName string) ([]string, []string, error) {
	schemaProperties, err := getSchemaProperties(schemaName)
	if err == nil {
		properties := make([]string, 0)
		sampleValues := make([]string, 0)
		for key, value := range schemaProperties {
			properties = append(properties, key)
			sampleValues = append(sampleValues, utils.GetSampleValueByType(value))
		}
		return properties, sampleValues, nil
	}
	return nil, nil, err
}

func getSchemaProperties(schemaName string) (spec.SchemaProperties, error) {
	registrySwaggerSpecification := getSwaggerJson()
	schemaDefinition, ok := registrySwaggerSpecification.Definitions[schemaName]
	if !ok {
		return nil, errors.New(schemaName + "schema not found")
	}
	return schemaDefinition.Properties, nil
}

func getSwaggerJson() spec.Swagger {
	resp, err := http.Get(config.Config.Registry.BaseUrl + "api/docs/swagger.json")
	utils.LogErrorIfAny("Error creating a get request for %v : %v", err, config.Config.Registry.BaseUrl+"api/docs/swagger.json")
	body, _ := io.ReadAll(resp.Body)
	var responseMap spec.Swagger
	err = json.Unmarshal(body, &responseMap)
	utils.LogErrorIfAny("Error creating request body for %v : %v", err, config.Config.Registry.BaseUrl+"api/docs/swagger.json")
	return responseMap
}
