package pkg

import (
	"encoding/json"
	"metrics/config"
	"metrics/cron"
	"metrics/models"
	"metrics/swagger_gen/restapi/operations"
	"metrics/swagger_gen/restapi/operations/aggregates"
	"metrics/swagger_gen/restapi/operations/metrics"

	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func SetupHandlers(api *operations.MetricsAPI) {
	api.MetricsGetV1MetricsHandler = metrics.GetV1MetricsHandlerFunc(getAllMetrics)
	api.AggregatesGetV1AggregatesHandler = aggregates.GetV1AggregatesHandlerFunc(getWeeklyAggregates)
}

func getAllMetrics(params metrics.GetV1MetricsParams) middleware.Responder {
	response := metrics.NewGetV1MetricsOK()
	dbInstance := models.GetDBInstance(config.Config.Database.ProviderName)
	count := dbInstance.GetCount()
	response.SetPayload(count)
	return response
}

func getWeeklyAggregates(params aggregates.GetV1AggregatesParams) middleware.Responder {
	response := aggregates.NewGetV1AggregatesOK()
	aggregatesStr, err := cron.CronObj.GetWeeklyAggregates()
	if err != nil {
		response.WithPayload("Failed")
	}
	var aggregates map[string]map[string]string
	if err := json.Unmarshal([]byte(aggregatesStr), &aggregates); err != nil {
		log.Errorf("Error while unmarshalling aggregates : %v", err)
	}
	response.SetPayload(aggregates)
	return response
}
