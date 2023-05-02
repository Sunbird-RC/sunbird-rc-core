package config

import "github.com/jinzhu/configor"

var Config = struct {
	Keycloak struct {
		TokenURL     string `env:"KEYCLOAK_TOKEN_URL" default:"http://localhost/token"`
		ClientId     string `env:"KEYCLOAK_CLIENT_ID" default:"admin-api"`
		ClientSecret string `env:"KEYCLOAK_CLIENT_SECRET" default:"**"`
	}
	Digilocker struct {
		IDPrefix    string `env:"DIGILOCKER_DOC_ID_PREFIX" default:"dev.sunbirdrc.vc"`
		AuthKeyName string `env:"DIGILOCKER_AUTH_KEYNAME" default:"x-digilocker-hmac"`
		AuthHMACKey string `env:"DIGILOCKER_HMAC_AUTHKEY" default:"***"`
	}
	Registry struct {
		URL            string `env:"REGISTRY_URL" default:"https://demo-education-registry.xiv.in/registry/"`
		SchemaMapper   string `env:"SCHEMA_MAPPER" default:"{\"doctype\": \"Schema\"}"`
		TemplateMapper string `env:"TEMPLATE_MAPPER" default:"{\"doctype\": \"template\"}"`
	}
	LogLevel string `env:"LOG_LEVEL" yaml:"log_level" default:"DEBUG"`
	Host     string `env:"HOST" yaml:"host" default:"0.0.0.0"`
	Port     string `env:"PORT" yaml:"port" default:"8085"`
	MODE     string `env:"MODE" yaml:"mode" default:"debug" `
}{}

func Init() {
	err := configor.Load(&Config, "./config/application-default.yml") //"config/application.yml"
	if err != nil {
		panic("Unable to read configurations")
	}

}
