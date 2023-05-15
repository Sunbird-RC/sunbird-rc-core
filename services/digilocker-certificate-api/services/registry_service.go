package services

import (
	"bytes"
	"digilocker-certificate-api/config"
	"encoding/json"
	"errors"
<<<<<<< HEAD
	"text/template"
	"time"
=======
	"html/template"
	"strconv"
>>>>>>> 126d964b (Add digilocker-certificate-api by removing swagger and creating with gin framework)

	req "github.com/imroc/req/v3"
	log "github.com/sirupsen/logrus"
)

type TokenResponse struct {
	AccessToken string `json:"access_token"`
<<<<<<< HEAD
	ExpiresIn   int    `json:"expires_in"`
}

type RegistryService struct {
}

type SearchResult struct {
	Osid string `json:"osid"`
}

func (service RegistryService) getEntityOsid(schema string, templateStr string, docDetails map[string]string) (string, error) {
	searchTemplate := template.Must(template.New("").Parse(templateStr))
	buf := bytes.Buffer{}
	if err := searchTemplate.Execute(&buf, docDetails); err == nil {
		log.Debugf("Calling Search API with %v request body", buf.String())
=======
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
>>>>>>> 126d964b (Add digilocker-certificate-api by removing swagger and creating with gin framework)
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
<<<<<<< HEAD
=======
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
>>>>>>> 126d964b (Add digilocker-certificate-api by removing swagger and creating with gin framework)
	}
}

func (service RegistryService) getCertificate(pullUriRequest PullURIRequest) ([]byte, string, error) {
	docTypeMapper := config.SchemaDocTypeMapper[pullUriRequest.DocDetails["DocType"]].(map[string]interface{})
	schema := docTypeMapper["schema"].(string)
	template := docTypeMapper["template"].(string)
	log.Debugf("Schema %s", schema)
	log.Debugf("Template %s", template)
	client := req.C()

	token, err := service.getServiceAccountToken()
	log.Debugf("Token %s", token)
	if err != nil {
		return nil, "", err
	}
	searchFilter, err := json.Marshal(docTypeMapper["searchFilter"])
	if err != nil {
		log.Errorf("Error in marshalling searchFilter : %v", searchFilter)
	}
	log.Debugf("SearchFilter : %v", string(searchFilter))
	osid, err := service.getEntityOsid(schema, string(searchFilter), pullUriRequest.DocDetails)
	log.Debugf("Searched Entity OSID : %v", osid)
	if err != nil {
		return nil, "", err
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
		return nil, "", err
	}

	if resp.IsErrorState() {
		log.Error(resp.Dump())
		return nil, "", errors.New("received error response from registry" + resp.Dump())
	}

	log.Debugf("Registry API response, %v", resp.Dump())
	return resp.Bytes(), osid, nil
}

// TODO: cache token˳
func (service RegistryService) getServiceAccountToken() (string, error) {
	if token, found := config.CacheService.Get("clientSecretServiceToken"); found {
		log.Debug("In Cache")
		return token.(string), nil
	}
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
<<<<<<< HEAD
	config.CacheService.Set("clientSecretServiceToken", tokenResponse.AccessToken, time.Duration(tokenResponse.ExpiresIn)*time.Millisecond)
=======
>>>>>>> 126d964b (Add digilocker-certificate-api by removing swagger and creating with gin framework)
	log.Debugf("Keycloak API response, %v", resp.Dump())
	return tokenResponse.AccessToken, nil
}
