package consumers

import "github.com/divoc/notification-service/config"

func Init() {
	if config.Config.Kafka.Enable {
		go notifyConsumer()
	}
}
