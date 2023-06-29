package server

import (
	"digilocker-certificate-api/config"
	"fmt"
	log "github.com/sirupsen/logrus"
)

func Init() {
	r := NewRouter()
	err := r.Run(fmt.Sprintf("%s:%s", config.Config.Host, config.Config.Port))
	if err != nil {
		log.Error("Failed starting the router")
	}
}
