package config

import (
	"errors"
	"strings"

	"github.com/imroc/req"
	"github.com/jinzhu/configor"
	log "github.com/sirupsen/logrus"
)

type config struct {
	Registry struct {
		BaseUrl string `env:"REGISTRY_BASE_URL" yaml:"baseUrl" default:"http://localhost:8081/"`
	}
	Keycloak struct {
		PublicKey string `env:"KEYCLOAK_PUBLIC_KEY" yaml:"publicKey" default:""`
		Url       string `env:"KEYCLOAK_URL" yaml:"url" default:"http://keycloak:8080/auth"`
		Realm     string `env:"KEYCLOAK_REALM" yaml:"realm" default:"sunbird-rc"`
	}
	Database struct {
		Host     string `env:"DATABASE_HOST" yaml:"host" default:"localhost"`
		Port     string `env:"DATABASE_PORT" yaml:"port" default:"5432"`
		User     string `env:"DATABASE_USER" yaml:"user" default:"postgres"`
		Password string `env:"DATABASE_PASSWORD" yaml:"password" default:"postgres"`
		DBName   string `env:"DATABASE_NAME" yaml:"dbName" default:"registry"`
	}
	LogLevel string `env:"LOG_LEVEL" yaml:"log_level" default:"info"`
	Roles    string `env:"ROLES" yaml:"roles" default:""`
}

var Config = config{}

func (c config) GetRoles() []string {
	if len(strings.Trim(c.Roles, " ")) > 0 {
		return strings.Split(c.Roles, ",")
	} else {
		return []string{}
	}
}

func Initialize(fileName string) {
	err := configor.Load(&Config, fileName)
	if err != nil {
		panic("Unable to read configurations")
	}
	if Config.Keycloak.PublicKey == "" {
		err := getPublicKeyFromKeycloak()
		if err != nil {
			log.Error("Error while fetching public key from keycloak", err)
		}
	}
}

func getPublicKeyFromKeycloak() error {
	url := Config.Keycloak.Url + "/realms/" + Config.Keycloak.Realm
	log.Infof("Public key url : %v", url)
	resp, err := req.Get(url)
	if err != nil {
		return err
	}
	log.Infof("Got response %+v", resp.String())
	responseObject := map[string]interface{}{}
	if err := resp.ToJSON(&responseObject); err == nil {
		if publicKey, ok := responseObject["public_key"].(string); ok {
			Config.Keycloak.PublicKey = publicKey
		}
	}
	if Config.Keycloak.PublicKey == "" {
		return errors.New("unable to get public key from keycloak")
	}
	return nil
}
