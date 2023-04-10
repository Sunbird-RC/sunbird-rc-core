package pkg

import (
	"bulk_issuance/swagger_gen/restapi/operations"
	"bulk_issuance/swagger_gen/restapi/operations/download_file_report"
	"bulk_issuance/swagger_gen/restapi/operations/sample_template"
	"bulk_issuance/swagger_gen/restapi/operations/upload_and_create_records"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"
)

func SetupHandlers(api *operations.BulkIssuanceAPI) {
	api.SampleTemplateGetV1SampleSchemaNameHandler = sample_template.GetV1SampleSchemaNameHandlerFunc(downloadSampleFile)
	api.UploadedFilesGetV1UploadedFilesHandler = uploaded_files.GetV1UploadedFilesHandlerFunc(listFiles)
	api.DownloadFileReportGetV1DownloadIDHandler = download_file_report.GetV1DownloadIDHandlerFunc(downloadReportFile)
	api.UploadAndCreateRecordsPostV1UploadFilesVCNameHandler = upload_and_create_records.PostV1UploadFilesVCNameHandlerFunc(createRecords)
}
