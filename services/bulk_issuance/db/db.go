package db

import (
	"bulk_issuance/config"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/utils"
	"errors"
	"fmt"

	log "github.com/sirupsen/logrus"

	"github.com/jinzhu/gorm"
	_ "github.com/jinzhu/gorm/dialects/postgres"
)

type IRepo interface {
	GetUploadedFileByIdAndUserId(id int, userId string) (*UploadedFile, error)
	Insert(data *UploadedFile) (uint, error)
	GetAllUploadedFilesByUserId(userId string, pagination Pagination) ([]UploadedFile, error)
}

type Repository struct {
	db *gorm.DB
}

type Pagination struct {
	Limit  int64 `json:"limit"`
	Offset int64 `json:"page"`
}

type UploadedFile struct {
	gorm.Model
	Filename     string
	TotalRecords int
	UserID       string
	UserName     string
	Headers      string
	RowData      []byte
	Date         string
}

func (f UploadedFile) ToDTO() *models.UploadedFileDTO {
	return &models.UploadedFileDTO{
		ID:          int64(f.ID),
		CreatedAt:   f.CreatedAt.String(),
		Date:        f.Date,
		Filename:    f.Filename,
		TotalRecord: int64(f.TotalRecords),
		UpdatedAt:   f.UpdatedAt.String(),
		UserID:      f.UserID,
		Username:    f.UserName,
	}
}

func Init() IRepo {
	var e error
	repo := Repository{}
	dbPath := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		config.Config.Database.Host, config.Config.Database.Port,
		config.Config.Database.User, config.Config.Database.Password, config.Config.Database.DBName,
	)
	log.Infof("Using db %s", dbPath)
	repo.db, e = gorm.Open("postgres", dbPath)
	if e != nil {
		panic("failed to connect to database")
	}
	repo.db.AutoMigrate(&UploadedFile{})
	return &repo
}

func (repo *Repository) GetUploadedFileByIdAndUserId(id int, userId string) (*UploadedFile, error) {
	filesUpload := &UploadedFile{}
	log.Infof("Getting file data with id : %v", id)
	result := repo.db.First(&filesUpload, "id=? AND user_id=?", id, userId)
	if result.Error != nil {
		log.Errorf("Error while getting UploadedFile : %v", result.Error)
		return nil, result.Error
	}
	return filesUpload, nil
}

func (repo *Repository) Insert(data *UploadedFile) (uint, error) {
	log.Info("Creating UploadedFile entry")
	result := repo.db.Create(&data)
	utils.LogErrorIfAny("Error while adding UploadedFile : %v", result.Error)
	return data.ID, nil
}

func (repo *Repository) GetAllUploadedFilesByUserId(userId string, pagination Pagination) ([]UploadedFile, error) {
	var files []UploadedFile
	log.Info("Getting all uploaded files")
	if err := repo.db.
		Limit(pagination.Limit).
		Offset(pagination.Offset).
		Find(&files, "user_id = ?", userId).
		Error; err != nil {
		log.Errorf("Error while requesting for all uploaded files : %v", err)
		return nil, errors.New("no files uploaded")
	}
	return files, nil
}
