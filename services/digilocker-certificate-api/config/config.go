package config

import (
	"encoding/json"
	"fmt"
	"io"
	"os"

	"github.com/jinzhu/configor"
)

var Config = struct {
	Keycloak struct {
		TokenURL     string `env:"KEYCLOAK_TOKEN_URL" default:"http://localhost:8080/auth/realms/sunbird-rc/protocol/openid-connect/token"`
		ClientId     string `env:"KEYCLOAK_CLIENT_ID" default:"admin-api"`
		ClientSecret string `env:"KEYCLOAK_CLIENT_SECRET" default:"2b65f70c-bd02-4d5c-95d9-5341225aa849"`
	}
	Digilocker struct {
		IDPrefix    string `env:"DIGILOCKER_DOC_ID_PREFIX" default:"org.upsmfac"`
		AuthKeyName string `env:"DIGILOCKER_AUTH_KEYNAME" default:"x-digilocker-hmac"`
		AuthHMACKey string `env:"DIGILOCKER_HMAC_AUTHKEY" default:"84600d73-e618-4c80-a347-6b51147103ee"`
	}
	Registry struct {
		URL string `env:"REGISTRY_URL" default:"https://localhost:8081/"`
	}
	LogLevel string `env:"LOG_LEVEL" yaml:"log_level" default:"DEBUG"`
	Host     string `env:"HOST" yaml:"host" default:"0.0.0.0"`
	Port     string `env:"PORT" yaml:"port" default:"8085"`
	MODE     string `env:"MODE" yaml:"mode" default:"debug" `
}{}

var SchemaDocTypeMapper map[string]interface{}

func Init() {
	err := configor.Load(&Config, "./config/application-default.yml") //"config/application.yml"
	if err != nil {
		panic("Unable to read configurations")
	}
	jsonFile, err := os.Open("./config/docType.json")
	if err != nil {
		fmt.Printf("Error : %v", err)
	}
	str, err := io.ReadAll(jsonFile)
	if err != nil {
		fmt.Printf("Error occurred while reading json file :  %v", err)
	}
	if err := json.Unmarshal([]byte(str), &SchemaDocTypeMapper); err != nil {
		fmt.Printf("Error Unmarshalling Json file: %v", err)
	}
	defer jsonFile.Close()
}
