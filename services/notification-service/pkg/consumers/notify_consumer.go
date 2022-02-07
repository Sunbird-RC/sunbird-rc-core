package consumers

import (
	"encoding/json"
	"fmt"
	"github.com/sunbirdrc/notification-service/config"
	"github.com/sunbirdrc/notification-service/pkg/services"
	"github.com/sunbirdrc/notification-service/swagger_gen/models"
	log "github.com/sirupsen/logrus"
	"gopkg.in/confluentinc/confluent-kafka-go.v1/kafka"
)

func notifyConsumer() {
	c, err := kafka.NewConsumer(&kafka.ConfigMap{
		"bootstrap.servers":  config.Config.Kafka.BootstrapServers,
		"group.id":           "notifier",
		"auto.offset.reset":  "earliest",
		"enable.auto.commit": "false",
	})

	if err != nil {
		log.Error(err)
	}
	err = c.SubscribeTopics([]string{config.Config.Kafka.NotifyTopic}, nil)
	if err != nil {
		log.Error(err)
	} else {
		for {
			msg, err := c.ReadMessage(-1)
			if err == nil {
				fmt.Printf("Message on %s: %s\n", msg.TopicPartition, string(msg.Value))
				var request models.NotificationRequest
				if err := json.Unmarshal([]byte(string(msg.Value)), &request); err != nil {
					log.Errorf("Received message is not in required format %+v", err)
				}
				contactType, err := services.GetContactType(*request.Recipient)
				if contactType == services.SMS {
					mobileNumber, err := services.GetMobileNumber(*request.Recipient)
					if err == nil {
						if resp, err := services.SendSMS(mobileNumber, *request.Message); err == nil {
							log.Debugf("SMS sent response %+v", resp)

						} else {
							log.Errorf("Error in sending SMS %+v", err)
						}
					} else {
						log.Errorf("Invalid notification mobile number %+v, %+v", request, err)
					}
				} else if contactType == services.Email {
					emailId, err := services.GetEmailId(*request.Recipient)
					if err == nil {
						if err := services.SendEmail(emailId, request.Subject, *request.Message); err == nil {
							log.Debugf("Email sent successfully")

						} else {
							log.Errorf("Error in sending SMS %+v", err)
						}
					} else {
						log.Errorf("Invalid notification mobile number %+v, %+v", request, err)
					}
				} else {
					log.Errorf("Invalid notification contact %+v, %+v", request, err)
				}

			} else {
				// The client will automatically try to recover from all errors.
				fmt.Printf("Consumer error: %v \n", err)
			}
			if msg != nil {
				_, err = c.CommitMessage(msg)
			}
			if err != nil {
				log.Errorf("Error in committing message %+v", err)
			}
		}

		c.Close()
	}
}
