package services

import "bulk_issuance/db"

func(services *Services) ListFileForUser(userId string) ([]db.FileData, error) {
	return services.repo.GetAllFileDataForUserID(userId)
}