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

type FileData struct {
	gorm.Model
	Filename     string
	TotalRecords int
	UserID       string
	UserName     string
	Headers      string
	RowData      []byte
	Date         string
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
	db.AutoMigrate(&FileData{})
}

func GetFileDataByIdAndUser(id int, userId string) (*FileData, error) {
	filesUpload := &FileData{}
	log.Infof("Getting file data with id : %v", id)
	result := db.First(&filesUpload, "id=?", id)
	if result.Error != nil {
		log.Errorf("Error while getting FileData : %v", result.Error)
		return nil, result.Error
	}
	return filesUpload, nil
}

func Insert(data *FileData) (uint, error) {
	log.Info("Creating FileData entry")
	result := db.Create(&data)
	utils.LogErrorIfAny("Error while adding FileData : %v", result.Error)
	return data.ID, nil
}

func GetAllFileDataForUserID(userId string) ([]FileData, error) {
	var files []FileData
	log.Info("Getting all uploaded files")
	if err := db.Find(&files, "user_id = ?", userId).Error; err != nil {
		log.Errorf("Error while requesting for all uploaded files : %v", err)
		return nil, errors.New("No files uploaded")
	}
	return files, nil
}
