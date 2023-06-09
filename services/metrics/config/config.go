package config

import (
	"github.com/jinzhu/configor"
)

func Initialize(fileName string) {
	err := configor.Load(&Config, fileName)
	if err != nil {
		panic("Unable to read configurations")
	}
}

var Config = struct {
	Clickhouse struct {
		Dsn      string `env:"CLICK_HOUSE_URL" yaml:"dsn" default:"clickhouse:9000"`
		Database string `env:"CLICKHOUSE_DATABASE" yaml:"database" default:"default"`
	}
	Kafka struct {
		BootstrapServers    string `env:"KAFKA_BOOTSTRAP_SERVERS" yaml:"bootstrapServers" default:"localhost:9094"`
		KAFKA_METRICS_TOPIC string `env:"KAFKA_METRICS_TOPIC" yaml:"metricsTopic" default:"metrics"`
	}
	Database struct {
		ProviderName string `env:"DATABASE_PROVIDER_NAME" yaml:"providerName" default:"clickhouse"`
	}
	Redis struct {
		Url string `env:"REDIS_URL" yaml:"url" default:"redis:6379"`
	}
	Cron struct {
		Enable bool `env:"CRON_ENABLE" yaml:"enable" default:"true"`
		ScheduleInterval int `env:"SCHEDULE_INTERVAL" yaml:"scheduleInterval" default:"7"`
		ScheduleTime string `env:"SCHEDULE_TIME" yaml:"scheduleTime" default:"00:00"`
	}
}{}
