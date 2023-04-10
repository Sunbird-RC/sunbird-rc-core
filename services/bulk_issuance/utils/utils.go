package utils

import (
	"bulk_issuance/config"
	"encoding/json"
	"errors"
	"github.com/lucasjones/reggen"
	"io"
	"net/http"
	"sort"
	"time"

	log "github.com/sirupsen/logrus"
)

func Contains(arr []string, str string) bool {
	for _, a := range arr {
		if a == str {
			return true
		}
	}
	return false
}

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

func GetSchemaPropertiesAndSampleValues(key string) ([]string, []string, error) {
	schemaProperties, err := getJsonSchemaProperties(key)
	if err == nil {
		properties := make([]string, 0)
		sampleValues := make([]string, 0)
		for key, value := range schemaProperties {
			properties = append(properties, key)
			sampleValues = append(sampleValues, getSampleValueByType(value.(map[string]interface{})))
		}
		return properties, sampleValues, nil
	}
	return nil, nil, err
}

func getJsonSchemaProperties(key string) (map[string]interface{}, error) {
	resp, err := http.Get(config.Config.Registry.BaseUrl + "api/docs/swagger.json")
	LogErrorIfAny("Error creating a get request for %v : %v", err, config.Config.Registry.BaseUrl+"api/docs/swagger.json")
	body, _ := io.ReadAll(resp.Body)
	var responseMap map[string]interface{}
	err = json.Unmarshal(body, &responseMap)
	LogErrorIfAny("Error creating request body for %v : %v", err, config.Config.Registry.BaseUrl+"api/docs/swagger.json")
	schemaMap := responseMap["definitions"].(map[string]interface{})[key]
	if schemaMap == nil {
		return nil, errors.New(key + " not found")
	}
	schemaProperties := schemaMap.(map[string]interface{})["properties"].(map[string]interface{})
	return schemaProperties, nil
}

func getSampleValueByType(value map[string]interface{}) string {
	if jsonType, ok := value["type"]; ok {
		switch jsonType {
		case "string":
			if format, ok := value["format"]; ok {
				return getSampleStringValueBasedOnFormat(format.(string))
			}
			if pattern, ok := value["pattern"]; ok {
				if randomString, err := generateRandomString(pattern); err == nil {
					return randomString
				}
			}
		case "number":
			return "0"
		case "integer":
			return "0"
		case "object":
			return "{}"
		case "array":
			return "[]"
		case "boolean":
			return "true"
		}
	}
	return "string"
}

func generateRandomString(pattern interface{}) (string, error) {
	return reggen.Generate(pattern.(string), 1)
}

func getSampleStringValueBasedOnFormat(format string) string {
	switch format {
	case "date":
		currentTime := time.Now()
		return currentTime.Format(time.DateOnly)
	case "date-time":
		currentTime := time.Now()
		return currentTime.Format(time.RFC3339)
	case "email":
		return "yyy@xx.com"
	}
	return ""
}

func LogErrorIfAny(description string, err error, data ...interface{}) {
	if err != nil {
		log.Errorf(description, data, err)
	}
}
