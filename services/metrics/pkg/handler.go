package pkg

import (
	"metrics/config"
	"metrics/models"
	"metrics/swagger_gen/restapi/operations"
	"metrics/swagger_gen/restapi/operations/metrics"

	"github.com/go-openapi/runtime/middleware"
)

func SetupHandlers(api *operations.MetricsAPI) {
	api.MetricsGetV1MetricsHandler = metrics.GetV1MetricsHandlerFunc(getAllMetrics)
}

func getAllMetrics(params metrics.GetV1MetricsParams) middleware.Responder {
	response := metrics.NewGetV1MetricsOK()
	dbInstance := models.GetDBInstance(config.Config.Database.ProviderName)
	count := dbInstance.GetCount()
	response.SetPayload(count)
	return response
}
