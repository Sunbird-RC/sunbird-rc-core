package kafka

import (
	"context"
	"encoding/json"
	"metrics/config"
	"metrics/models"
	"strings"

	"github.com/segmentio/kafka-go"
	log "github.com/sirupsen/logrus"
)

func StartConsumer(servers string, groupId string, autoOffsetReset string, autoCommit string, c models.IDatabase) {
	log.Infof("Kafka bootstrap servers: %v", servers)
	brokers := strings.Split(servers, ",")
	topic := config.Config.Kafka.KAFKA_METRICS_TOPIC

	r := kafka.NewReader(kafka.ReaderConfig{
		Brokers:     brokers,
		GroupID:     groupId,
		Topic:       topic,
		StartOffset: kafka.LastOffset,
	})
	defer r.Close()

	ctx := context.Background()
	for {
		msg, err := r.FetchMessage(ctx)
		if err != nil {
			log.Errorf("Error fetching message from Kafka: %v", err)
			continue
		}
		var metricData models.Metrics
		if err = json.Unmarshal(msg.Value, &metricData); err != nil {
			log.Errorf("Error unmarshalling metric data: %v", err)
		} else {
			c.InsertRecord(metricData)
		}
		if err = r.CommitMessages(ctx, msg); err != nil {
			log.Errorf("Error committing Kafka message: %v", err)
		}
	}
}
