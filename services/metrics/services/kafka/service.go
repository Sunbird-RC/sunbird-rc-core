package kafka

import (
	"encoding/json"
	"metrics/config"
	"metrics/models"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	log "github.com/sirupsen/logrus"
)

func StartConsumer(servers string, groupId string, autoOffsetReset string, autoCommit string, c models.IDatabase) {
	log.Infof("%v", servers)
	consumer, err := kafka.NewConsumer(&kafka.ConfigMap{
		"bootstrap.servers":  servers,
		"group.id":           groupId,
		"auto.offset.reset":  autoOffsetReset,
		"enable.auto.commit": autoCommit,
	})
	if err != nil {
		panic(err)
	}
	consumer.SubscribeTopics([]string{config.Config.Kafka.KAFKA_METRICS_TOPIC}, nil)
	ReadMessage(consumer, c)
	consumer.Close()
}

func ReadMessage(consumer *kafka.Consumer, c models.IDatabase) {
	for {
		var metricData models.Metrics
		msg, err := consumer.ReadMessage(-1)
		if err != nil {
			log.Fatal(err)
		}
		if err = json.Unmarshal(msg.Value, &metricData); err != nil {
			log.Errorf("%v", err)
		}
		c.InsertRecord(metricData)
		consumer.CommitMessage(msg)
	}
}
