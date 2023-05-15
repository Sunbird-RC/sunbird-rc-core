package services

import (
	"crypto/hmac"
	"crypto/sha256"
	"digilocker-certificate-api/config"
	"encoding/base64"
	"encoding/hex"
	"encoding/xml"
	"errors"

	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
)

type PullURIRequest struct {
	XMLName    xml.Name `xml:"PullURIRequest"`
	Text       string   `xml:",chardata"`
	Ns2        string   `xml:"ns2,attr"`
	Ver        string   `xml:"ver,attr"`
	Ts         string   `xml:"ts,attr"`
	Txn        string   `xml:"txn,attr"`
	OrgId      string   `xml:"orgId,attr"`
	Format     string   `xml:"format,attr"`
	DocDetails struct {
		Text          string `xml:",chardata"`
		DocType       string `xml:"DocType"`
		DigiLockerId  string `xml:"DigiLockerId"`
		UID           string `xml:"UID"`
		FullName      string `xml:"FullName"`
		DOB           string `xml:"DOB"`
		Photo         string `xml:"Photo"`
		TrackingId    string `xml:"tracking_id"`
		Mobile        string `xml:"Mobile"`
		CertificateId string `xml:"certificate_id"`
		UDF1          string `xml:"UDF1"`
		UDF2          string `xml:"UDF2"`
		UDF3          string `xml:"UDF3"`
		UDFn          string `xml:"UDFn"`
	} `xml:"DocDetails"`
}

type PullURIResponse struct {
	XMLName        xml.Name `xml:"PullURIResponse"`
	Text           string   `xml:",chardata"`
	Ns2            string   `xml:"ns2,attr"`
	ResponseStatus struct {
		Text   string `xml:",chardata"`
		Status string `xml:"Status,attr"`
		Ts     string `xml:"ts,attr"`
		Txn    string `xml:"txn,attr"`
	} `xml:"ResponseStatus"`
	DocDetails struct {
		Text         string `xml:",chardata"`
		DocType      string `xml:"DocType"`
		DigiLockerId string `xml:"DigiLockerId"`
		UID          string `xml:"UID"`
		FullName     string `xml:"FullName"`
		DOB          string `xml:"DOB"`
		TrackingId   string `xml:"tracking_id"`
		Mobile       string `xml:"Mobile"`
		UDF1         string `xml:"UDF1"`
		URI          string `xml:"URI"`
		DocContent   string `xml:"DocContent"`
		DataContent  string `xml:"DataContent"`
	} `xml:"DocDetails"`
}

type DigiLockerService struct {
	registryService RegistryService
}

func (service DigiLockerService) ProcessURIRequest(context *gin.Context) (any, error) {
	rawData, err := context.GetRawData()
	if err != nil {
		log.Error("Failed while reading request body", err)
		return nil, err
	}
	actualHMAC, err := service.generateHMAC(rawData)
	if err != nil {
		return nil, err
	}
	hmacFromRequest, err := service.getHMACFromRequest(context)
	if err != nil {
		return nil, err
	}
	validRequest := service.validateHMAC(actualHMAC, hmacFromRequest)
	if validRequest {
		var pullUriRequest PullURIRequest
		err := xml.Unmarshal(rawData, &pullUriRequest)
		if err != nil {
			log.Error("Failed while unmarshalling request body", err)
			return nil, err
		}
		certificate, err := service.registryService.getCertificate(pullUriRequest.DocDetails.DocType,
			pullUriRequest.DocDetails.CertificateId)
		if err != nil {
			return nil, err
		}
		pullURIResponse := service.generatePullURIResponse(pullUriRequest, certificate)
		log.Debugf("pullURIResponse %v", pullURIResponse)
		return pullURIResponse, nil
	} else {
		return nil, errors.New("invalid / unauthorized access")
	}
}

func (service DigiLockerService) validateHMAC(actualHMAC []byte, expectedHMAC []byte) bool {
	return hmac.Equal(actualHMAC, expectedHMAC)
}

func (service DigiLockerService) generateHMAC(rawData []byte) ([]byte, error) {
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

func (service DigiLockerService) getHMACFromRequest(context *gin.Context) ([]byte, error) {
	hmacDigest := context.GetHeader(config.Config.Digilocker.AuthKeyName)
	hmacSignByteArray, err := base64.StdEncoding.DecodeString(hmacDigest)
	if err != nil {
		log.Error("Error while decoding hmac digest", err)
		return nil, err
	}
	return hmacSignByteArray, nil
}

func (service DigiLockerService) generatePullURIResponse(request PullURIRequest, certificate []byte) PullURIResponse {
	response := PullURIResponse{}
	response.ResponseStatus.Ts = request.Ts
	response.ResponseStatus.Txn = request.Txn
	response.DocDetails.URI = config.Config.Digilocker.IDPrefix + "-" + request.DocDetails.DocType + "-" + request.DocDetails.CertificateId
	response.ResponseStatus.Status = "1"
	response.DocDetails.DocType = request.DocDetails.DocType
	response.DocDetails.DigiLockerId = request.DocDetails.DigiLockerId
	response.DocDetails.FullName = request.DocDetails.FullName
	response.DocDetails.DOB = request.DocDetails.DOB
	response.DocDetails.DocContent = base64.StdEncoding.EncodeToString(certificate)
	return response
}
