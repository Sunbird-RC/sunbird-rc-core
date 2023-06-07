package models

import (
	"context"
	"metrics/config"
	"metrics/utils"
	"strconv"
	"strings"

	"github.com/ClickHouse/clickhouse-go/v2"
	log "github.com/sirupsen/logrus"
)

type Clickhouse struct {
	connection clickhouse.Conn
}

func (c *Clickhouse) InitDB() {
	dbConnectionInfo := config.Config.Clickhouse.Dsn
	log.Infof("Clickhouse URL : %v", dbConnectionInfo)
	connect, err := clickhouse.Open(&clickhouse.Options{
		Addr: []string{dbConnectionInfo},
		Auth: clickhouse.Auth{
			Database: config.Config.Clickhouse.Database,
		},
		Settings: clickhouse.Settings{
			"allow_experimental_object_type": 1,
		},
	})
	c.connection = connect
	if err != nil {
		log.Fatal(err)
	}
}

func createTableIfNotExists(tableName string, c *Clickhouse) error {
	ctx := context.Background()
	createTableCmd := `CREATE TABLE IF NOT EXISTS ` + strings.ToLower(tableName) + ` (
		operationType 	String,
		entity			JSON,
		date 			Date,
		id 				String
	) ENGINE = MergeTree() order by date`
	err := c.connection.Exec(ctx, createTableCmd)
	if err != nil {
		log.Fatal(err)
	}
	return err
}

func (c *Clickhouse) InsertRecord(metricData Metrics) error {
	ctx := context.Background()
	createTableIfNotExists(metricData.Object.Type, c)
	batch, err := c.connection.PrepareBatch(ctx, "INSERT INTO "+strings.ToLower(metricData.Object.Type))
	if err != nil {
		log.Fatal(err)
		return err
	}
	metricTime, _ := utils.GetTimeFromMilliseconds(metricData.Ets)
	err = batch.Append(metricData.Eid, metricData.Edata, metricTime, metricData.Object.Id)
	if err != nil {
		log.Fatal(err)
		return err
	}
	err = batch.Send()
	if err != nil {
		log.Fatal(err)
		return err
	}
	log.Info("Insert successful")
	return nil
}

func (c *Clickhouse) GetCount() map[string]string {
	ctx := context.Background()
	tables, err := getTables(c, ctx)
	if err != nil {
		log.Errorf("Error occurred while fetching table names : %v", err)
	}
	mapping := map[string]string{}
	for i := range tables {
		var query string
		query = "SELECT count(*) FROM " + tables[i]
		rows, err := c.connection.Query(ctx, query)
		if err != nil {
			log.Fatal(err)
		}
		var count *uint64
		for rows.Next() {
			if err := rows.Scan(&count); err != nil {
				log.Fatal(err)
			}
		}
		mapping[tables[i]] = strconv.FormatUint(*count, 10)
	}
	return mapping
}

func getTables(c *Clickhouse, ctx context.Context) ([]string, error) {
	rows, err := c.connection.Query(ctx, "SHOW TABLES")
	if err != nil {
		return nil, err
	}
	tables := []string{}
	for rows.Next() {
		var tableName string
		if err := rows.Scan(&tableName); err != nil {
			log.Fatal(err)
		}
		tables = append(tables, tableName)
	}
	return tables, nil
}

func (c *Clickhouse) GetAggregates(clauses string) map[string]string {
	ctx := context.Background()
	tables, err := getTables(c, ctx)
	if err != nil {
		log.Errorf("Error occurred while fetching table names : %v", err)
	}
	mapping := map[string]string{}
	for i := range tables {
		query := "SELECT count(*) FROM " + tables[i] + " " + clauses
		log.Debugf("Query : %v", query)
		rows, err := c.connection.Query(ctx, query)
		if err != nil {
			log.Fatal(err)
		}
		var count *uint64
		for rows.Next() {
			if err := rows.Scan(&count); err != nil {
				log.Fatal(err)
			}
		}
		mapping[tables[i]] = strconv.FormatUint(*count, 10)
	}
	return mapping
}
