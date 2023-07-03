package server

import (
	"digilocker-certificate-api/config"
	"digilocker-certificate-api/controllers"
	"digilocker-certificate-api/middlewares"
	"github.com/gin-gonic/gin"
)

func NewRouter() *gin.Engine {
	gin.SetMode(config.Config.MODE)
	router := gin.New()
	router.Use(gin.Logger())
	router.Use(gin.Recovery())

	health := new(controllers.HealthController)

	router.GET("/health", health.Status)
	router.Use(middlewares.AuthMiddleware())

	v1 := router.Group("v1")
	{
		digilockerGroup := v1.Group("digilocker")
		{
			var digilocker controllers.Digilocker
			digilocker.Init()
			digilockerGroup.POST("/pullUriRequest", digilocker.PullURIRequest)
		}
	}
	return router

}
