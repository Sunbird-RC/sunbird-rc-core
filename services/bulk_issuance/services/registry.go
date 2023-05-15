package services

import (
	"bulk_issuance/config"
	"bulk_issuance/utils"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"sort"
)

func GetSchemaProperties(key string) ([]string, error) {
	schemaProperties, err := getJsonSchemaProperties(key)
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

func getSchemaPropertiesAndSampleValues(key string) ([]string, []string, error) {
	schemaProperties, err := getJsonSchemaProperties(key)
	if err == nil {
		properties := make([]string, 0)
		sampleValues := make([]string, 0)
		for key, value := range schemaProperties {
			properties = append(properties, key)
			sampleValues = append(sampleValues, utils.GetSampleValueByType(value.(map[string]interface{})))
		}
		return properties, sampleValues, nil
	}
	return nil, nil, err
}

func getJsonSchemaProperties(key string) (map[string]interface{}, error) {
	resp, err := http.Get(config.Config.Registry.BaseUrl + "api/docs/swagger.json")
	utils.LogErrorIfAny("Error creating a get request for %v : %v", err, config.Config.Registry.BaseUrl+"api/docs/swagger.json")
	body, _ := io.ReadAll(resp.Body)
	var responseMap map[string]interface{}
	err = json.Unmarshal(body, &responseMap)
	utils.LogErrorIfAny("Error creating request body for %v : %v", err, config.Config.Registry.BaseUrl+"api/docs/swagger.json")
	schemaMap := responseMap["definitions"].(map[string]interface{})[key]
	if schemaMap == nil {
		return nil, errors.New(key + " not found")
	}
	schemaProperties := schemaMap.(map[string]interface{})["properties"].(map[string]interface{})
	return schemaProperties, nil
}
