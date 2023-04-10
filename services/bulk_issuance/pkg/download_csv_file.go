package pkg

import (
	"bulk_issuance/db"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations/download_file_report"
	"bulk_issuance/utils"
	"bytes"
	"encoding/csv"
	"encoding/json"
	"strings"

	"github.com/go-openapi/runtime/middleware"
	log "github.com/sirupsen/logrus"
)

func downloadReportFile(params download_file_report.GetV1DownloadIDParams, principal *models.JWTClaimBody) middleware.Responder {
	log.Infof("Downloading report file with ID : %v", params.ID)
	response := download_file_report.GetV1DownloadFileNameOK{}
	file, err := db.GetDBFileData(int(params.ID))
	if err != nil {
		return download_file_report.NewGetV1DownloadIDNotFound().WithPayload(err.Error())
	}
	var data [][]string
	err = json.Unmarshal(file.RowData, &data)
	utils.LogErrorIfAny("Error while unmarshalling row data for downloading report of file : %v ", err)
	data = append([][]string{strings.Split(file.Headers, ",")}, data...)
	b := new(bytes.Buffer)
	w := csv.NewWriter(b)
	w.WriteAll(data)
	utils.LogErrorIfAny("Error while opening a file with name %v : %v ", err, file.Filename)
	response.WithContentDisposition("attachment; filename=\"" + file.Filename + "\"").WithPayload(b)
	log.Infof("Downloading file with name : %v", file.Filename)
	return &response
}
