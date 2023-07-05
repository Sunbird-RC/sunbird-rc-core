package models

type IDatabase interface {
	InitDB()
	InsertRecord(metricData Metrics) error
	GetCount() map[string]map[string]string
	GetAggregates(whereClauseCondition string) map[string]map[string]string
}

var clickhouseObj = &Clickhouse{}
var dbMaps = map[string]IDatabase{
	"clickhouse": clickhouseObj,
}

func GetDBInstance(name string) IDatabase {
	return dbMaps[name]
}
