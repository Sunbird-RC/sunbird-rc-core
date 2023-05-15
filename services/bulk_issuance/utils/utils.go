package utils

import (
	"github.com/go-openapi/spec"
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

func GetSampleValueByType(value spec.Schema) string {
	if len(value.Type) > 0 {
		jsonType := value.Type[0]
		switch jsonType {
		case "string":
			if format := value.Format; len(format) > 0 {
				return getSampleStringValueBasedOnFormat(format)
			}
			if pattern := value.Pattern; len(pattern) > 0 {
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
