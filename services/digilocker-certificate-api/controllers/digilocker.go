package controllers

import (
	"digilocker-certificate-api/services"
	"net/http"

	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
)

type Digilocker struct {
	service services.DigiLockerService
}
func (handler *Digilocker) Init() {
	handler.service.Init()
}

func (handler Digilocker) PullURIRequest(c *gin.Context) {
	log.Info("PullURIRequest triggered")
	response, err := handler.service.ProcessURIRequest(c)
	if response == nil {
		log.Error("Error while processing URI request", err)
		c.JSON(http.StatusInternalServerError, err)
	} else {
		c.XML(http.StatusOK, response)
	}
}
