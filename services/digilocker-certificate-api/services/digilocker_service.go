package services

import (
	"crypto/hmac"
	"crypto/sha256"
	"digilocker_certificate_api/config"
	"digilocker_certificate_api/swagger_gen/models"
	"encoding/base64"
	"encoding/hex"
	"errors"

	log "github.com/sirupsen/logrus"
)

type DigiLockerService struct {
	registryService RegistryService
}

func Init() DigiLockerService {
	digilockerService := DigiLockerService{
		registryService: RegistryService{},
	}
	return digilockerService
}

func (service DigiLockerService) GenerateHMAC(rawData []byte) ([]byte, error) {
	key := ([]byte)(config.Config.Digilocker.AuthHMACKey)
	mac := hmac.New(sha256.New, key)
	mac.Write(rawData)
	macBytes := mac.Sum(nil)
	hexMac := ([]byte)(hex.EncodeToString(macBytes))
	if hexMac == nil {
		return nil, errors.New("failed generating hmac for payload")
	}
	return hexMac, nil
}

func (service DigiLockerService) GetHMACFromRequest(hmacDigest string) ([]byte, error) {
	hmacSignByteArray, err := base64.StdEncoding.DecodeString(hmacDigest)
	if err != nil {
		log.Error("Error while decoding hmac digest", err)
		return nil, err
	}
	return hmacSignByteArray, nil
}

func (service DigiLockerService) ValidateHMAC(actualHMAC []byte, expectedHMAC []byte) bool {
	return hmac.Equal(actualHMAC, expectedHMAC)
}

func (service DigiLockerService) GetCertificate(docType string, certificateId string) ([]byte, error) {
	return service.registryService.GetCertificate(docType, certificateId)
}

func (service DigiLockerService) GeneratePullURIResponse(request models.PullURIRequest, certificate []byte) models.PullURIRequestOKResponse {
	response := models.PullURIRequestOKResponse{}
	response.ResponseStatus.Ts = request.Ts
	response.ResponseStatus.Txn = request.Txn
	response.DocDetails.URI = config.Config.Digilocker.IDPrefix + "-" + request.DocDetails.DocType + "-" + request.DocDetails.CertificateID
	response.ResponseStatus.Status = "1"
	response.DocDetails.DocType = request.DocDetails.DocType
	response.DocDetails.DigiLockerID = request.DocDetails.DigiLockerID
	response.DocDetails.FullName = request.DocDetails.FullName
	response.DocDetails.DOB = request.DocDetails.DOB
	response.DocDetails.DocContent = base64.StdEncoding.EncodeToString(certificate)
	return response
}
