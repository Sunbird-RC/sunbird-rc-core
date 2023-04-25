package utils

import (
	"time"

	"github.com/lucasjones/reggen"
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

func LogErrorIfAny(description string, err error, data ...interface{}) {
	if err != nil {
		log.Errorf(description, data, err)
	}
}

func GetSampleValueByType(value map[string]interface{}) string {
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