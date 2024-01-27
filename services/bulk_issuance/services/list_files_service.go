package services

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
)

func (services *Services) GetUploadedFiles(userId string, limit *int64, offset *int64) ([]*models.UploadedFileDTO, error) {
	filesUploaded, err := services.repo.GetAllUploadedFilesByUserId(userId, db.Pagination{
		Limit:  *limit,
		Offset: *offset,
	})
	uploadedFileReport := make([]*models.UploadedFileDTO, 0)
	for _, file := range filesUploaded {
		uploadedFileReport = append(uploadedFileReport, file.ToDTO())
	}
	return uploadedFileReport, err
}
