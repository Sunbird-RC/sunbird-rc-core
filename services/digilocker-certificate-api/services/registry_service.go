package services

import (
	"bytes"
	"digilocker-certificate-api/config"
	"encoding/json"
	"errors"
	"text/template"
	"time"

	req "github.com/imroc/req/v3"
	"github.com/patrickmn/go-cache"
	log "github.com/sirupsen/logrus"
)

type TokenResponse struct {
	AccessToken string `json:"access_token"`
	ExpiresIn   int    `json:"expires_in"`
}

type RegistryService struct {
	cacheService *cache.Cache
}

func (service *RegistryService) Init() {
	service.cacheService = cache.New(12*time.Hour, 24*time.Hour)
}

type SearchResult struct {
	Osid string `json:"osid"`
}

func (service RegistryService) getEntityOsid(schema string, templateStr string, docDetails map[string]string) (string, error) {
	searchTemplate := template.Must(template.New("").Parse(templateStr))
	buf := bytes.Buffer{}
	if err := searchTemplate.Execute(&buf, docDetails); err == nil {
		log.Debugf("Calling Search API with %v request body", buf.String())
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

func (service RegistryService) getCertificate(pullUriRequest PullURIRequest) ([]byte, string, error) {
	docTypeMapper := config.SchemaDocTypeMapper[pullUriRequest.DocDetails["DocType"]].(map[string]interface{})
	schema := docTypeMapper["schema"].(string)
	schemaTemplate := docTypeMapper["template"].(string)
	log.Debugf("Schema %s", schema)
	log.Debugf("Template %s", schemaTemplate)
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
	resp, err := client.R().
		SetHeader("Accept", "application/pdf").
		SetHeader("template-key", schemaTemplate).
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

func (service RegistryService) getServiceAccountToken() (string, error) {
	if token, found := service.cacheService.Get("clientSecretServiceToken"); found {
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
	service.cacheService.Set("clientSecretServiceToken", tokenResponse.AccessToken, time.Duration(tokenResponse.ExpiresIn)*time.Millisecond)
	log.Debugf("Keycloak API response, %v", resp.Dump())
	return tokenResponse.AccessToken, nil
}