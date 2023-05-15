package services

import (
	"bytes"
	"digilocker-certificate-api/config"
	"encoding/json"
	"errors"
	"html/template"
	"strconv"

	req "github.com/imroc/req/v3"
	log "github.com/sirupsen/logrus"
)

type TokenResponse struct {
	AccessToken string `json:"access_token"`
}

type RegistryService struct {
}

type SearchResult struct {
	Osid string `json:"osid"`
}

func (service RegistryService) getEntityOsid(schema string, parameters ...string) (string, error) {
	templateStr, err := service.getENVMapper(schema, config.Config.Registry.SearchBodyMapper)
	if err != nil {
		return "", err
	}
	searchTemplate := template.Must(template.New("").Parse(templateStr))
	buf := bytes.Buffer{}
	parameterMap := make(map[string]string)
	for i, parameter := range parameters {
		parameterMap["parameter"+strconv.Itoa(i+1)] = parameter
	}
	log.Debugf("ParameterMap : %v", parameterMap)
	if err = searchTemplate.Execute(&buf, parameterMap); err == nil {
		client := req.C()
		resp, err := client.R().
			SetHeader("Content-Type", "application/json").
			SetBody(buf.Bytes()).
			Post(config.Config.Registry.URL + "api/v1/" + schema + "/search")
		if err != nil {
			return "", err
		}
		var responseBody []SearchResult
		err = json.Unmarshal(resp.Bytes(), &responseBody)
		if err == nil && len(responseBody) > 0 {
			return responseBody[0].Osid, nil
		}
		return "", err
	} else {
		return "", err
	}
}

func (service RegistryService) getCertificate(documentType string, certificateId string) ([]byte, error) {
	schema, err := service.getSchemaMapper(documentType)
	if err != nil {
		return nil, err
	}
	template, err := service.getTemplateMapper(documentType)
	if err != nil {
		return nil, err
	}
	log.Debugf("Schema %s", schema)
	log.Debugf("Template %s", template)
	client := req.C()

	token, err := service.getServiceAccountToken()
	log.Debugf("Token %s", token)
	if err != nil {
		return nil, err
	}
	osid, err := service.getEntityOsid(schema, certificateId)
	if err != nil {
		return nil, err
	}
	resp, err := client.R().
		SetHeader("Accept", "application/pdf").
		SetHeader("template-key", template).
		SetBearerAuthToken(token).
		SetPathParam("schema", schema).
		SetPathParam("osid", osid).
		Get(config.Config.Registry.URL + "api/v1/{schema}/{osid}")
	if err != nil {
		log.Error("error:", err)
		log.Error("raw content:")
		log.Debugf(resp.Dump())
		return nil, err
	}

	if resp.IsErrorState() {
		log.Error(resp.Dump())
		return nil, errors.New("received error response from registry" + resp.Dump())
	}

	log.Debugf("Registry API response, %v", resp.Dump())
	return resp.Bytes(), nil
}

func (service RegistryService) getSchemaMapper(documentType string) (string, error) {
	return service.getENVMapper(documentType, config.Config.Registry.SchemaMapper)
}

func (service RegistryService) getENVMapper(documentType string, envMapper string) (string, error) {
	var schemaMapper map[string]string
	err := json.Unmarshal([]byte(envMapper), &schemaMapper)
	if err != nil {
		log.Error("Failed parsing mapper config", err)
		return "", err
	}
	schema, found := schemaMapper[documentType]
	if found {
		return schema, nil
	} else {
		return "", errors.New("no mapping found for document type: " + documentType)
	}
}

// TODO: cache token˳
func (service RegistryService) getServiceAccountToken() (string, error) {
	log.Infof("Get service account token")
	client := req.C()
	var tokenResponse TokenResponse
	resp, err := client.R().
		SetHeader("Content-Type", "application/x-www-form-urlencoded").
		SetFormData(map[string]string{
			"grant_type":    "client_credentials",
			"client_id":     config.Config.Keycloak.ClientId,
			"client_secret": config.Config.Keycloak.ClientSecret,
		}).
		SetSuccessResult(&tokenResponse).
		Post(config.Config.Keycloak.TokenURL)

	if err != nil {
		log.Error("error:", err)
		log.Error("raw content:")
		log.Debugf(resp.Dump())
		return "", err
	}
	if resp.IsErrorState() {
		log.Error(resp.Dump())
		return "", errors.New("received error response from keycloak token api" + resp.Dump())
	}
	log.Debugf("Keycloak API response, %v", resp.Dump())
	return tokenResponse.AccessToken, nil
}

func (service RegistryService) getTemplateMapper(documentType string) (string, error) {
	return service.getENVMapper(documentType, config.Config.Registry.TemplateMapper)
}
