package services

import (
	"bulk_issuance/swagger_gen/models"
)

func (services *Services) GetUploadedFiles(userId string) ([]*models.UploadedFiles, error) {
	filesUploaded, err := services.repo.GetAllFileDataForUserID(userId)
	uploadedFileReport := make([]*models.UploadedFiles, 0)
	for _, file := range filesUploaded {
		uploadedFileReport = append(uploadedFileReport, file.ToDTO())
	}
	return uploadedFileReport, err
}
