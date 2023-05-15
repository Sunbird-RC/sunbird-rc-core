package main

import (
	"digilocker-certificate-api/config"
	"digilocker-certificate-api/server"
	"fmt"
	log "github.com/sirupsen/logrus"
)

func main() {
	config.Init()
	ll, err := log.ParseLevel(config.Config.LogLevel)
	if err != nil {
		fmt.Print("Failed parsing log level")
	}
	log.SetLevel(ll)
	log.Info("Log level: %s", config.Config.LogLevel)
	server.Init()
}
