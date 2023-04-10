package db

import (
	"bulk_issuance/config"
	"bulk_issuance/utils"
	"errors"
	"fmt"

	log "github.com/sirupsen/logrus"

	"github.com/jinzhu/gorm"
	_ "github.com/jinzhu/gorm/dialects/postgres"
)

var db *gorm.DB

type DBFiles struct {
	gorm.Model
	Filename     string
	TotalRecords int
	UserID       string
	Date         string
}

type DBFileData struct {
	gorm.Model
	Filename string
	Headers  string
	RowData  []byte
}

func Init() {
	var e error
	dbPath := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		config.Config.Database.Host, config.Config.Database.Port,
		config.Config.Database.User, config.Config.Database.Password, config.Config.Database.DBName,
	)
	log.Infof("Using db %s", dbPath)
	db, e = gorm.Open("postgres", dbPath)
	if e != nil {
		panic("failed to connect to database")
	}
	db.AutoMigrate(&DBFiles{})
	db.AutoMigrate(&DBFileData{})
}

func CreateDBFiles(data *DBFiles) error {
	log.Info("Creating DBFiles entry")
	result := db.Create(&data)
	utils.LogErrorIfAny("Error while adding DBFiles : %v", result.Error)
	return nil
}

func GetDBFileData(id int) (*DBFileData, error) {
	filesUpload := &DBFileData{}
	log.Infof("Getting file data with id : %v", id)
	result := db.First(&filesUpload, "id=?", id)
	if result.Error != nil {
		log.Errorf("Error while getting DBFileData : %v", result.Error)
		return nil, result.Error
	}
	return filesUpload, nil
}

func CreateDBFileData(data *DBFileData) (uint, error) {
	log.Info("Creating DBFileData entry")
	result := db.Create(&data)
	utils.LogErrorIfAny("Error while adding DBFileData : %v", result.Error)
	return data.ID, nil
}

func GetAllUploadedFilesData() ([]DBFiles, error) {
	var files []DBFiles
	log.Info("Getting all uploaded files")
	if err := db.Find(&files).Error; err != nil {
		log.Errorf("Error while requesting for all uploaded files : %v", err)
		return nil, errors.New("No files uploaded")
	}
	return files, nil
}
