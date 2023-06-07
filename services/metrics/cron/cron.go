package cron

import (
	"context"
	"encoding/json"
	"metrics/config"
	"metrics/models"
	"time"

	"github.com/go-co-op/gocron"
	redislock "github.com/go-co-op/gocron-redis-lock"
	"github.com/redis/go-redis/v9"

	log "github.com/sirupsen/logrus"
)

type Cron struct {
	db          *models.IDatabase
	redisClient *redis.Client
}

var CronObj *Cron

func Init(database *models.IDatabase) {
	cron := Cron{
		db: database,
	}
	redisOptions := &redis.Options{
		Addr: config.Config.Redis.Url,
	}
	cron.redisClient = redis.NewClient(redisOptions)
	locker, err := redislock.NewRedisLocker(cron.redisClient, redislock.WithTries(1))
	if err != nil {
		log.Errorf("Failed Gaining Lock Because : %v", err)
	}

	cronJob := gocron.NewScheduler(time.UTC)
	cronJob.WithDistributedLocker(locker)
	_, err = cronJob.Every(1).Day().At("19:30").Do(cron.SaveWeeklyMetrics, database)
	if err != nil {
		log.Errorf("Failed Creating a scheduler : %v", err)
	}
	CronObj = &cron
}

func (cron *Cron) SaveWeeklyMetrics(db models.IDatabase) {
	days := 7
	backDatedDate := time.Now().Add(-1 * time.Duration(24*time.Duration(days)*time.Hour)).Format("2006-01-02")
	whereClause := "WHERE date > '" + backDatedDate + "'"
	aggregate := db.GetAggregates(whereClause)
	bytes, err := json.Marshal(aggregate)
	if err != nil {
		log.Infof("Error in marshalling : %v", err)
	}
	ctx := context.Background()
	log.Infof(string(bytes))
	err = cron.redisClient.Set(ctx, "weeklyUpdates", string(bytes), 24*time.Duration(days)*time.Hour).Err()
	if err != nil {
		log.Infof("Error in saving to redis : %v", err)
	}
	log.Info("Saved to redis")
}

func (cron *Cron) GetWeeklyAggregates() (string, error) {
	ctx := context.Background()
	return cron.redisClient.Get(ctx, "weeklyUpdates").Result()
}
