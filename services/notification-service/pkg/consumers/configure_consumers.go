package consumers

import "github.com/sunbirdrc/notification-service/config"

func Init() {
	if config.Config.Kafka.Enable {
		go notifyConsumer()
	}
}
